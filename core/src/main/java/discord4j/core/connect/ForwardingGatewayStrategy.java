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

import discord4j.common.json.payload.dispatch.Dispatch;
import discord4j.core.event.dispatch.DispatchContext;
import discord4j.core.event.domain.Event;
import discord4j.gateway.GatewayClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Function;

public class ForwardingGatewayStrategy implements GatewayStrategy {

    private final PayloadSink sink;
    private final PayloadSource source;

    public ForwardingGatewayStrategy(PayloadSink sink, PayloadSource source) {
        this.sink = sink;
        this.source = source;
    }

    @Override
    public Flux<Dispatch> dispatchSource(GatewayClient gatewayClient) {
        sink.send(gatewayClient.receiver())
                .subscribeOn(Schedulers.newSingle("payload-sender"))
                .log().subscribe();
        return Flux.empty(); // no dispatch source under this strategy
    }

    @Override
    public Function<DispatchContext<Dispatch>, Mono<Event>> eventMapper() {
        return context -> Mono.empty(); // no-op
    }

//    @Nullable
//    @Override
//    public FluxSink<GatewayPayload<?>> dispatchSink(GatewayClient gatewayClient) {
//        FluxSink<GatewayPayload<?>> senderSink = gatewayClient.sender();
//        source.receive(payload -> {
//            if (senderSink.isCancelled()) {
//                return Mono.error(new IllegalStateException("Sender was cancelled"));
//            }
//            senderSink.next(payload);
//            return Mono.empty();
//        }).log().subscribe();
//        return senderSink;
//    }
}
