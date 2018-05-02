/*
 * This file is part of Discord4J.
 *
 * Discord4J is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Discord4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Discord4J. If not, see <http://www.gnu.org/licenses/>.
 */

package discord4j.core.connect;

import discord4j.common.json.payload.Opcode;
import discord4j.common.json.payload.dispatch.Dispatch;
import discord4j.core.event.dispatch.DispatchContext;
import discord4j.core.event.dispatch.DispatchHandlers;
import discord4j.core.event.domain.Event;
import discord4j.gateway.GatewayClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

import java.util.function.Function;

public class ReceivingGatewayStrategy implements GatewayStrategy {

    private final PayloadSource source;
    private final PayloadSink sink;

    public ReceivingGatewayStrategy(PayloadSource source, PayloadSink sink) {
        this.source = source;
        this.sink = sink;
    }

    @Override
    public Flux<Dispatch> dispatchSource(GatewayClient gatewayClient) {
        UnicastProcessor<Dispatch> dispatch = UnicastProcessor.create();
        FluxSink<Dispatch> dispatchSink = dispatch.sink(FluxSink.OverflowStrategy.LATEST);
        source.receive(payload -> {
            if (Opcode.DISPATCH.equals(payload.getOp()) && payload.getData() != null) {
                dispatchSink.next((Dispatch) payload.getData());
            }
            return Mono.empty();
        }).log().subscribe();
        return dispatch;
    }

    @Override
    public Function<DispatchContext<Dispatch>, Mono<Event>> eventMapper() {
        return DispatchHandlers::<Dispatch, Event>handle;
    }

//    @Nullable
//    @Override
//    public FluxSink<GatewayPayload<?>> dispatchSink(GatewayClient gatewayClient) {
//        return null;
//    }
}
