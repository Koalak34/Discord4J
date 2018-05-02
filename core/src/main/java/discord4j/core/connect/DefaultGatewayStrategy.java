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
import discord4j.core.event.dispatch.DispatchHandlers;
import discord4j.core.event.domain.Event;
import discord4j.gateway.GatewayClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class DefaultGatewayStrategy implements GatewayStrategy {

    @Override
    public Flux<Dispatch> dispatchSource(GatewayClient gatewayClient) {
        return gatewayClient.dispatch();
    }

    @Override
    public Function<DispatchContext<Dispatch>, Mono<Event>> eventMapper() {
        return DispatchHandlers::<Dispatch, Event>handle;
    }

//    @Override
//    public FluxSink<GatewayPayload<?>> dispatchSink(GatewayClient gatewayClient) {
//        return gatewayClient.sender();
//    }
}
