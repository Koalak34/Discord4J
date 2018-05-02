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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Discord4J.  If not, see <http://www.gnu.org/licenses/>.
 */
package discord4j.core;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import discord4j.common.jackson.PossibleModule;
import discord4j.core.connect.DefaultGatewayStrategy;
import discord4j.core.connect.GatewayStrategy;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.dispatch.DispatchContext;
import discord4j.core.event.domain.Event;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.IdentifyOptions;
import discord4j.gateway.payload.JacksonPayloadReader;
import discord4j.gateway.payload.JacksonPayloadWriter;
import discord4j.gateway.retry.RetryOptions;
import discord4j.rest.RestClient;
import discord4j.rest.http.*;
import discord4j.rest.http.client.SimpleHttpClient;
import discord4j.rest.request.Router;
import discord4j.rest.route.Routes;
import discord4j.store.service.StoreService;
import discord4j.store.service.StoreServiceLoader;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Objects;

public final class ClientBuilder {

    private String token;
    private StoreService storeService;
    private FluxProcessor<Event, Event> eventProcessor;
    private Scheduler eventScheduler;
    private RetryOptions retryOptions;
    private IdentifyOptions identifyOptions;
    private GatewayStrategy gatewayStrategy;

    public ClientBuilder(final String token) {
        this.token = Objects.requireNonNull(token);
        this.storeService = new StoreServiceLoader().getStoreService();
        this.eventProcessor = EmitterProcessor.create(false);
        this.eventScheduler = Schedulers.elastic();
        this.retryOptions = new RetryOptions(Duration.ofSeconds(2), Duration.ofSeconds(120), Integer.MAX_VALUE);
        this.identifyOptions = new IdentifyOptions();
        this.gatewayStrategy = new DefaultGatewayStrategy();
    }

    public String getToken() {
        return token;
    }

    public ClientBuilder setToken(final String token) {
        this.token = Objects.requireNonNull(token);
        return this;
    }

    public StoreService getStoreService() {
        return storeService;
    }

    public ClientBuilder setStoreService(final StoreService storeService) {
        this.storeService = Objects.requireNonNull(storeService);
        return this;
    }

    public FluxProcessor<Event, Event> getEventProcessor() {
        return eventProcessor;
    }

    public ClientBuilder setEventProcessor(final FluxProcessor<Event, Event> eventProcessor) {
        this.eventProcessor = Objects.requireNonNull(eventProcessor);
        return this;
    }

    public Scheduler getEventScheduler() {
        return eventScheduler;
    }

    public ClientBuilder setEventScheduler(final Scheduler eventScheduler) {
        this.eventScheduler = Objects.requireNonNull(eventScheduler);
        return this;
    }

    public RetryOptions getRetryOptions() {
        return retryOptions;
    }

    public void setRetryOptions(RetryOptions retryOptions) {
        this.retryOptions = retryOptions;
    }

    public IdentifyOptions getIdentifyOptions() {
        return identifyOptions;
    }

    public void setIdentifyOptions(IdentifyOptions identifyOptions) {
        this.identifyOptions = identifyOptions;
    }

    public GatewayStrategy getGatewayStrategy() {
        return gatewayStrategy;
    }

    public void setGatewayStrategy(GatewayStrategy gatewayStrategy) {
        this.gatewayStrategy = gatewayStrategy;
    }

    public DiscordClient build() {
        final ObjectMapper mapper = new ObjectMapper()
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .registerModules(new PossibleModule(), new Jdk8Module());

        final SimpleHttpClient httpClient = SimpleHttpClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "Bot " + token)
                .defaultHeader("User-Agent", "Discord4J")
                .readerStrategy(new JacksonReaderStrategy<>(mapper))
                .readerStrategy(new EmptyReaderStrategy())
                .writerStrategy(new MultipartWriterStrategy(mapper))
                .writerStrategy(new JacksonWriterStrategy(mapper))
                .writerStrategy(new EmptyWriterStrategy())
                .baseUrl(Routes.BASE_URL)
                .build();

        final GatewayClient gatewayClient = new GatewayClient(
                new JacksonPayloadReader(mapper), new JacksonPayloadWriter(mapper),
                retryOptions, token, identifyOptions);

        final StoreHolder storeHolder = new StoreHolder(storeService);
        final RestClient restClient = new RestClient(new Router(httpClient));
        final ClientConfig config = new ClientConfig(token, identifyOptions);
        final EventDispatcher eventDispatcher = new EventDispatcher(eventProcessor, eventScheduler);

        final ServiceMediator serviceMediator = new ServiceMediator(gatewayClient, restClient, storeHolder,
                eventDispatcher, config);

        gatewayStrategy.dispatchSource(serviceMediator.getGatewayClient())
                .map(dispatch -> DispatchContext.of(dispatch, serviceMediator))
                .flatMap(gatewayStrategy.eventMapper())
                .subscribeWith(eventProcessor);

        return serviceMediator.getClient();
    }
}
