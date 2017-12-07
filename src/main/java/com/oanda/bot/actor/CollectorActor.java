package com.oanda.bot.actor;

import akka.actor.UntypedAbstractActor;
import com.google.common.collect.Iterables;
import com.oanda.bot.domain.Candle;
import com.oanda.bot.domain.Instrument;
import com.oanda.bot.domain.Step;
import com.oanda.bot.repository.CandleRepository;
import com.oanda.bot.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Scope("prototype")
@Component("CollectorActor")
public class CollectorActor extends UntypedAbstractActor {

    private final Instrument instrument;
    private final Step step;


    @Autowired
    private AccountService accountService;

    @Autowired
    private CandleRepository candleRepository;

    public CollectorActor(Instrument instrument, Step step) {
        this.instrument = instrument;
        this.step = step;
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof Messages.WorkTime) {
            context().parent().forward(message, context());
        }

        if (Messages.WORK.equals(message)) {
            List<Candle> candles = accountService.getCandles(instrument, step, getLastCollect())
                    .getCandles()
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!candles.isEmpty()) {
                log.info("Candle list size: {}", candles.size());
                candleRepository.addCandles(instrument, step, candles);
                getContext().getParent().tell(Iterables.getLast(candles), sender());
            }
        }
    }

    private DateTime getLastCollect() {
        Candle lastCandle = candleRepository.getLastCandle(instrument, step);
        return lastCandle == null
                ? DateTime.now(DateTimeZone.getDefault()).minusDays(2)
                : lastCandle.getTime();
    }
}
