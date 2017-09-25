/*
 * Copyright (c) 2015 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.enzo.roundlcdclock;


import eu.hansolo.enzo.fonts.Fonts;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;


/**
 * User: hansolo
 * Date: 27.12.13
 * Time: 07:05
 */
public class RoundLcdClock extends Region {
    private static final double                PREFERRED_WIDTH  = 200;
    private static final double                PREFERRED_HEIGHT = 200;
    private static final double                MINIMUM_WIDTH    = 25;
    private static final double                MINIMUM_HEIGHT   = 25;
    private static final double                MAXIMUM_WIDTH    = 1024;
    private static final double                MAXIMUM_HEIGHT   = 1024;
    private volatile ScheduledFuture<?>        periodicClockTask;
    private static   ScheduledExecutorService  periodicClockExecutorService;
    private          double                    size;
    private          double                    width;
    private          double                    height;
    private          ObjectProperty<Color>     hColor;
    private          ObjectProperty<Color>     mColor;
    private          ObjectProperty<Color>     m5Color;
    private          ObjectProperty<Color>     sColor;
    private          ObjectProperty<Color>     timeColor;
    private          ObjectProperty<Color>     dateColor;
    private          BooleanProperty           alarmOn;
    private          ObjectProperty<LocalTime> alarm;
    private          BooleanProperty           dateVisible;
    private          BooleanProperty           alarmVisible;
    private          Pane                      pane;
    private          Canvas                    canvasBkg;
    private          GraphicsContext           ctxBkg;
    private          Canvas                    canvasFg;
    private          GraphicsContext           ctxFg;
    private          Canvas                    canvasHours;
    private          GraphicsContext           ctxHours;
    private          Canvas                    canvasMinutes;
    private          GraphicsContext           ctxMinutes;
    private          Canvas                    canvasSeconds;
    private          GraphicsContext           ctxSeconds;
    private          Font                      font;
    private          IntegerProperty           hours;
    private          IntegerProperty           minutes;
    private          IntegerProperty           seconds;
    private          boolean                   pm;
    private          StringBuilder             time;


    // ******************** Constructors **************************************
    public RoundLcdClock() {
        hColor       = new SimpleObjectProperty<>(this, "hourColor", Color.BLACK);
        mColor       = new SimpleObjectProperty<>(this, "minuteColor", Color.rgb(0, 0, 0, 0.5));
        m5Color      = new SimpleObjectProperty<>(this, "5MinuteColor", Color.BLACK);
        sColor       = new SimpleObjectProperty<>(this, "secondColor", Color.BLACK);
        timeColor    = new SimpleObjectProperty<>(this, "timeColor", Color.BLACK);
        dateColor    = new SimpleObjectProperty<>(this, "dateColor", Color.BLACK);
        alarmOn      = new SimpleBooleanProperty(this, "alarmOn", false);
        alarm        = new SimpleObjectProperty<>(LocalTime.now().minusMinutes(1));
        dateVisible  = new SimpleBooleanProperty(this, "dateVisible", false);
        alarmVisible = new SimpleBooleanProperty(this, "alarmVisible", false);
        time         = new StringBuilder();
        hours        = new SimpleIntegerProperty(this, "hours", -1);
        minutes      = new SimpleIntegerProperty(this, "minutes", -1);
        seconds      = new SimpleIntegerProperty(this, "seconds", -1);
        init();
        initGraphics();
        registerListeners();
        scheduleClockTask();
    }


    // ******************** Initialization ************************************
    private void init() {
        if (Double.compare(getPrefWidth(), 0.0) <= 0 || Double.compare(getPrefHeight(), 0.0) <= 0 ||
            Double.compare(getWidth(), 0.0) <= 0 || Double.compare(getHeight(), 0.0) <= 0) {
            if (getPrefWidth() > 0 && getPrefHeight() > 0) {
                setPrefSize(getPrefWidth(), getPrefHeight());
            } else {
                setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
            }
        }

        if (Double.compare(getMinWidth(), 0.0) <= 0 || Double.compare(getMinHeight(), 0.0) <= 0) {
            setMinSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);
        }

        if (Double.compare(getMaxWidth(), 0.0) <= 0 || Double.compare(getMaxHeight(), 0.0) <= 0) {
            setMaxSize(MAXIMUM_WIDTH, MAXIMUM_HEIGHT);
        }
    }

    private void initGraphics() {
        canvasBkg     = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        ctxBkg        = canvasBkg.getGraphicsContext2D();

        canvasFg      = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        ctxFg         = canvasFg.getGraphicsContext2D();

        canvasHours   = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        ctxHours      = canvasHours.getGraphicsContext2D();

        canvasMinutes = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        ctxMinutes    = canvasMinutes.getGraphicsContext2D();

        canvasSeconds = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        ctxSeconds    = canvasSeconds.getGraphicsContext2D();

        pane      = new Pane();
        pane.getChildren().setAll(canvasBkg, canvasFg, canvasHours, canvasMinutes, canvasSeconds);

        getChildren().setAll(pane);
    }

    private void registerListeners() {
        widthProperty().addListener(o -> handleControlPropertyChanged("RESIZE"));
        heightProperty().addListener(o -> handleControlPropertyChanged("RESIZE"));
        hColor.addListener(o -> handleControlPropertyChanged("REDRAW"));
        mColor.addListener(o -> handleControlPropertyChanged("REDRAW"));
        m5Color.addListener(o -> handleControlPropertyChanged("REDRAW"));
        sColor.addListener(o -> handleControlPropertyChanged("REDRAW"));
        timeColor.addListener(o -> handleControlPropertyChanged("REDRAW"));
        dateColor.addListener(o -> handleControlPropertyChanged("REDRAW"));
        alarmOn.addListener(o -> handleControlPropertyChanged("REDRAW"));
        dateVisible.addListener(o -> handleControlPropertyChanged("REDRAW"));
        alarmVisible.addListener(o -> handleControlPropertyChanged("REDRAW"));
        hours.addListener((ov, o, n) -> handleControlPropertyChanged("HOURS"));
        minutes.addListener((ov, o, n) -> handleControlPropertyChanged("MINUTES"));
        seconds.addListener((ov, o, n) -> handleControlPropertyChanged("SECONDS"));
    }


    // ******************** Methods *******************************************
    protected void handleControlPropertyChanged(final String PROPERTY) {
        if ("RESIZE".equals(PROPERTY)) {
            resize();
        } else if ("REDRAW".equals(PROPERTY)) {
            stopTask(periodicClockTask);
            drawBackground();
            drawForeground();
            scheduleClockTask();
        } else if ("SECONDS".equals(PROPERTY)) {
            drawForeground();
            drawSeconds();
        } else if ("MINUTES".equals(PROPERTY)) {
            drawMinutes();
        } else if ("HOURS".equals(PROPERTY)) {
            drawHours();
        }
    }

    public final Color getHourColor() {
        return hColor.get();
    }
    public final void setHourColor(final Color HOUR_COLOR) {
        hColor.set(HOUR_COLOR);
    }
    public final ObjectProperty<Color> hourColorProperty() {
        return hColor;
    }

    public final Color getMinuteColor() {
        return mColor.get();
    }
    public final void setMinuteColor(final Color MINUTE_COLOR) {
        mColor.set(MINUTE_COLOR);
    }
    public final ObjectProperty<Color> minuteColorProperty() {
        return mColor;
    }

    public final Color getMinute5Color() {
        return m5Color.get();
    }
    public final void setMinute5Color(final Color MINUTE_5_COLOR) {
        m5Color.set(MINUTE_5_COLOR);
    }
    public final ObjectProperty<Color> minute5ColorProperty() {
        return m5Color;
    }

    public final Color getSecondColor() {
        return sColor.get();
    }
    public final void setSecondColor(final Color SECOND_COLOR) {
        sColor.set(SECOND_COLOR);
    }
    public final ObjectProperty<Color> secondColorProperty() {
        return sColor;
    }

    public final Color getTimeColor() {
        return timeColor.get();
    }
    public final void setTimeColor(final Color TIME_COLOR) {
        timeColor.set(TIME_COLOR);
    }
    public final ObjectProperty<Color> timeColorProperty() {
        return timeColor;
    }

    public final Color getDateColor() {
        return dateColor.get();
    }
    public final void setDateColor(final Color DATE_COLOR) {
        dateColor.set(DATE_COLOR);
    }
    public final ObjectProperty<Color> dateColorProperty() {
        return dateColor;
    }

    public final boolean isAlarmOn() {
        return alarmOn.get();
    }
    public final void setAlarmOn(final boolean ALARM_ON) {
        alarmOn.set(ALARM_ON);
    }
    public final BooleanProperty alarmOnProperty() {
        return alarmOn;
    }

    public final LocalTime getAlarm() {
        return alarm.get();
    }
    public final void setAlarm(final LocalTime ALARM) {
        alarm.set(ALARM);
    }
    public final ObjectProperty<LocalTime> alarmProperty() {
        return alarm;
    }

    public final boolean isDateVisible() {
        return dateVisible.get();
    }
    public final void setDateVisible(final boolean DATE_VISIBLE) {
        dateVisible.set(DATE_VISIBLE);
    }
    public final BooleanProperty dateVisibleProperty() {
        return dateVisible;
    }

    public final boolean isAlarmVisible() {
        return alarmVisible.get();
    }
    public final void setAlarmVisible(final boolean ALARM_VISIBLE) {
        alarmVisible.set(ALARM_VISIBLE);
    }
    public final BooleanProperty alarmVisibleProperty() {
        return alarmVisible;
    }

    public final void setColor(final Color COLOR) {
        setHourColor(COLOR);
        setMinuteColor(Color.color(COLOR.getRed(), COLOR.getGreen(), COLOR.getBlue(), 0.6));
        setMinute5Color(COLOR);
        setSecondColor(COLOR);
        setTimeColor(COLOR);
        setDateColor(COLOR);
    }

    private void fireAlarmEvent() {
        fireEvent(new AlarmEvent(this, this, AlarmEvent.ALARM));
    }

    private void drawForeground() {
        ctxFg.clearRect(0, 0, size, size);

        // draw the time
        ctxFg.setFill(getTimeColor());
        font = Fonts.digital(0.2 * size);
        ctxFg.setTextBaseline(VPos.CENTER);
        ctxFg.setTextAlign(TextAlignment.CENTER);
        ctxFg.setFont(font);
        ctxFg.fillText(time.toString(), size * 0.5, size * 0.5);

        // draw the date
        if (isDateVisible()) {
            ctxFg.setFill(getDateColor());
            font = Fonts.digital(0.09 * size);
            ctxFg.setFont(font);
            ctxFg.fillText(LocalDate.now().format(DateTimeFormatter.ISO_DATE), size * 0.5, size * 0.65);
        }

        // draw the alarmOn icon
        if (isAlarmVisible() &&isAlarmOn()) drawAlarmIcon(ctxFg, ctxFg.getFill());
    }

    private void drawHours() {
        int hourCounter = 1;
        double strokeWidth = size * 0.06;
        ctxHours.setLineCap(StrokeLineCap.BUTT);
        ctxHours.clearRect(0, 0, size, size);
        for (int i = 450 ; i >= 90 ; i--) {
            ctxHours.save();
            if (i % 30 == 0) {
                //draw hours
                ctxHours.setStroke(getHourColor());
                ctxHours.setLineWidth(strokeWidth);
                if (hours.get() == 0 || hours.get() == 12) {
                    ctxHours.strokeArc(strokeWidth * 0.5, strokeWidth * 0.5, size - strokeWidth, size - strokeWidth, i + 1 - 30, 28, ArcType.OPEN);
                } else if (hourCounter <= (pm ? hours.get() - 12 : hours.get())) {
                    ctxHours.strokeArc(strokeWidth * 0.5, strokeWidth * 0.5, size - strokeWidth, size - strokeWidth, i + 1 - 30, 28, ArcType.OPEN);
                    hourCounter++;
                }
            }
            ctxFg.restore();
        }
    }

    private void drawMinutes() {
        int minCounter  = 1;
        double strokeWidth = size * 0.06;
        ctxMinutes.setLineCap(StrokeLineCap.BUTT);
        ctxMinutes.clearRect(0, 0, size, size);
        for (int i = 450 ; i >= 90 ; i--) {
            ctxMinutes.save();
            if (i % 6 == 0) {
                // draw minutes
                if (minCounter <= minutes.get()) {
                    ctxMinutes.setStroke(minCounter % 5 == 0 ? getMinute5Color() :  getMinuteColor());
                    ctxMinutes.setLineWidth(strokeWidth);
                    ctxMinutes.strokeArc(strokeWidth * 0.5 + strokeWidth * 1.1, strokeWidth * 0.5 + strokeWidth * 1.1, size - strokeWidth - strokeWidth * 2.2, size - strokeWidth - strokeWidth * 2.2, i + 1 - 6, 4, ArcType.OPEN);
                    minCounter++;
                }
            }
            ctxMinutes.restore();
        }
    }

    private void drawSeconds() {
        int secCounter  = 1;
        double strokeWidth = size * 0.06;
        ctxSeconds.setLineCap(StrokeLineCap.BUTT);
        ctxSeconds.clearRect(0, 0, size, size);
        for (int i = 450 ; i >= 90 ; i--) {
            ctxSeconds.save();
            if (i % 6 == 0) {
                // draw seconds
                if (secCounter <= seconds.get() + 1) {
                    ctxSeconds.setStroke(getSecondColor());
                    ctxSeconds.setLineWidth(strokeWidth * 0.25);
                    ctxSeconds.strokeArc(strokeWidth * 0.5 + strokeWidth * 1.8, strokeWidth * 0.5 + strokeWidth * 1.8, size - strokeWidth - strokeWidth * 3.6, size - strokeWidth - strokeWidth * 3.6, i + 1 - 6, 4, ArcType.OPEN);
                    secCounter++;
                }
            }
            ctxSeconds.restore();
        }
    }

    private void drawBackground() {
        double strokeWidth = size * 0.06;
        ctxBkg.setLineCap(StrokeLineCap.BUTT);
        ctxBkg.clearRect(0, 0, size, size);
        // draw translucent background
        ctxBkg.setStroke(Color.rgb(0, 12, 6, 0.1));
        IntStream.range(0, 360).forEach(
            i -> {
                ctxBkg.save();
                if (i % 6 == 0) {
                    // draw minutes
                    ctxBkg.setStroke(Color.color(mColor.get().getRed(), mColor.get().getGreen(), mColor.get().getBlue(), 0.1));
                    ctxBkg.setLineWidth(strokeWidth);
                    ctxBkg.strokeArc(strokeWidth * 0.5 + strokeWidth * 1.1, strokeWidth * 0.5 + strokeWidth * 1.1, size - strokeWidth - strokeWidth * 2.2, size - strokeWidth - strokeWidth * 2.2, i + 1, 4, ArcType.OPEN);

                    // draw seconds
                    ctxBkg.setStroke(Color.color(sColor.get().getRed(), sColor.get().getGreen(), sColor.get().getBlue(), 0.1));
                    ctxBkg.setLineWidth(strokeWidth * 0.25);
                    ctxBkg.strokeArc(strokeWidth * 0.5 + strokeWidth * 1.8, strokeWidth * 0.5 + strokeWidth * 1.8, size - strokeWidth - strokeWidth * 3.6, size - strokeWidth - strokeWidth * 3.6, i + 1, 4, ArcType.OPEN);
                }
                if (i % 30 == 0) {
                    //draw hours
                    ctxBkg.setStroke(Color.color(hColor.get().getRed(), hColor.get().getGreen(), hColor.get().getBlue(), 0.1));
                    ctxBkg.setLineWidth(strokeWidth);
                    ctxBkg.strokeArc(strokeWidth * 0.5, strokeWidth * 0.5, size - strokeWidth, size - strokeWidth, i + 1, 28, ArcType.OPEN);
                }
                ctxBkg.restore();    
            }
        );        

        // draw the time
        ctxBkg.setFill(Color.color(timeColor.get().getRed(), timeColor.get().getGreen(), timeColor.get().getBlue(), 0.1));
        font = Fonts.digital(0.2 * size);
        ctxBkg.setTextBaseline(VPos.CENTER);
        ctxBkg.setTextAlign(TextAlignment.CENTER);
        ctxBkg.setFont(font);
        ctxBkg.fillText("88:88", size * 0.5, size * 0.5);

        // draw the date
        if (isDateVisible()) {
            ctxBkg.setFill(Color.color(dateColor.get().getRed(), dateColor.get().getGreen(), dateColor.get().getBlue(), 0.1));
            font = Fonts.digital(0.09 * size);
            ctxBkg.setFont(font);
            ctxBkg.fillText("8888-88-88", size * 0.5, size * 0.65);
        }

        // draw the alarmOn icon
        if (isAlarmVisible() && !isAlarmOn()) drawAlarmIcon(ctxBkg, ctxBkg.getFill());
    }

    private void drawAlarmIcon(final GraphicsContext CTX, final Paint COLOR) {
        double iconSize = 0.1 * size;
        CTX.save();
        CTX.translate((size - iconSize) * 0.5, size * 0.25);
        CTX.beginPath();
        CTX.moveTo(0.6875 * iconSize, 0.875 * iconSize);
        CTX.bezierCurveTo(0.625 * iconSize, 0.9375 * iconSize, 0.5625 * iconSize, iconSize, 0.5 * iconSize, iconSize);
        CTX.bezierCurveTo(0.4375 * iconSize, iconSize, 0.375 * iconSize, 0.9375 * iconSize, 0.375 * iconSize, 0.875 * iconSize);
        CTX.bezierCurveTo(0.375 * iconSize, 0.875 * iconSize, 0.6875 * iconSize, 0.875 * iconSize, 0.6875 * iconSize, 0.875 * iconSize);
        CTX.closePath();
        CTX.moveTo(iconSize, 0.8125 * iconSize);
        CTX.bezierCurveTo(0.6875 * iconSize, 0.5625 * iconSize, 0.9375 * iconSize, 0.0, 0.5 * iconSize, 0.0);
        CTX.bezierCurveTo(0.5 * iconSize, 0.0, 0.5 * iconSize, 0.0, 0.5 * iconSize, 0.0);
        CTX.bezierCurveTo(0.5 * iconSize, 0.0, 0.5 * iconSize, 0.0, 0.5 * iconSize, 0.0);
        CTX.bezierCurveTo(0.125 * iconSize, 0.0, 0.375 * iconSize, 0.5625 * iconSize, 0.0, 0.8125 * iconSize);
        CTX.bezierCurveTo(0.0, 0.8125 * iconSize, 0.0, 0.8125 * iconSize, 0.0, 0.8125 * iconSize);
        CTX.bezierCurveTo(0.0, 0.8125 * iconSize, 0.0, 0.8125 * iconSize, 0.0, 0.8125 * iconSize);
        CTX.bezierCurveTo(0.0, 0.8125 * iconSize, 0.0, 0.8125 * iconSize, 0.0625 * iconSize, 0.8125 * iconSize);
        CTX.bezierCurveTo(0.0625 * iconSize, 0.8125 * iconSize, 0.5 * iconSize, 0.8125 * iconSize, 0.5 * iconSize, 0.8125 * iconSize);
        CTX.bezierCurveTo(0.5 * iconSize, 0.8125 * iconSize, iconSize, 0.8125 * iconSize, iconSize, 0.8125 * iconSize);
        CTX.bezierCurveTo(iconSize, 0.8125 * iconSize, iconSize, 0.8125 * iconSize, iconSize, 0.8125 * iconSize);
        CTX.bezierCurveTo(iconSize, 0.8125 * iconSize, iconSize, 0.8125 * iconSize, iconSize, 0.8125 * iconSize);
        CTX.bezierCurveTo(iconSize, 0.8125 * iconSize, iconSize, 0.8125 * iconSize, iconSize, 0.8125 * iconSize);
        CTX.closePath();
        CTX.setFill(COLOR);
        CTX.fill();
        CTX.restore();
    }

    private void updateClock() {
        time.setLength(0);

        LocalTime now = LocalTime.now();

        pm = now.get(ChronoField.AMPM_OF_DAY) == 1;
        hours.set(now.getHour());
        String hourString = Integer.toString(hours.get());
        if (hours.get() < 10) {
            time.append("0");
            time.append(hourString.substring(0, 1));
        } else {
            time.append(hourString.substring(0, 1));
            time.append(hourString.substring(1));
        }

        time.append(":");

        minutes.set(now.getMinute());
        String minutesString = Integer.toString(minutes.get());
        if (minutes.get() < 10) {
            time.append("0");
            time.append(minutesString.substring(0, 1));
        } else {
            time.append(minutesString.substring(0, 1));
            time.append(minutesString.substring(1));
        }

        seconds.set(now.getSecond());
        
        if (isAlarmOn() && now.isAfter(getAlarm()) && now.isBefore(getAlarm().plusNanos(105_000_000))) {
            fireAlarmEvent();
        }
    }


    // ******************** Scheduled tasks ***********************************
    private synchronized static void enableClockExecutorService() {
        if (null == periodicClockExecutorService) {
            periodicClockExecutorService = new ScheduledThreadPoolExecutor(1, getThreadFactory("RoundLcdClock", true));
        }
    }
    private synchronized void scheduleClockTask() {
        enableClockExecutorService();
        stopTask(periodicClockTask);
        periodicClockTask = periodicClockExecutorService.scheduleAtFixedRate(() -> {
            if (isVisible()) { Platform.runLater(() -> updateClock()); }
        }, 1, 1, TimeUnit.SECONDS);
    }
    private static ThreadFactory getThreadFactory(final String THREAD_NAME, final boolean IS_DAEMON) {
        return runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME);
            thread.setDaemon(IS_DAEMON);
            return thread;
        };
    }
    private void stopTask(ScheduledFuture<?> task) {
        if (null == task) return;

        task.cancel(true);
        task = null;
    }


    // ******************** Resizing ******************************************
    private void resize() {
        size   = getWidth() < getHeight() ? getWidth() : getHeight();
        width  = getWidth();
        height = getHeight();

        if (width > 0 && height > 0) {
            pane.setMaxSize(size, size);
            pane.relocate((getWidth() - size) * 0.5, (getHeight() - size) * 0.5);
            
            canvasBkg.setWidth(size);
            canvasBkg.setHeight(size);
            canvasFg.setWidth(size);
            canvasFg.setHeight(size);
            canvasHours.setWidth(size);
            canvasHours.setHeight(size);
            canvasMinutes.setWidth(size);
            canvasMinutes.setHeight(size);
            canvasSeconds.setWidth(size);
            canvasSeconds.setHeight(size);
            drawBackground();
            drawForeground();
            drawHours();
            drawMinutes();
            drawSeconds();
        }
    }
}
