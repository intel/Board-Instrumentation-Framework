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

package eu.hansolo.enzo.lcd.skin;

import eu.hansolo.enzo.fonts.Fonts;
import eu.hansolo.enzo.lcd.Alarm;
import eu.hansolo.enzo.lcd.LcdClock;
import javafx.collections.ListChangeListener;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Random;
import java.util.stream.IntStream;


/**
 * Created by
 * User: hansolo
 * Date: 19.08.13
 * Time: 08:14
 */
public class LcdClockSkin extends SkinBase<LcdClock> implements Skin<LcdClock> {
    private static final double      PREFERRED_WIDTH    = 380;
    private static final double      PREFERRED_HEIGHT   = 200;
    private static final double      MINIMUM_WIDTH      = 5;
    private static final double      MINIMUM_HEIGHT     = 5;
    private static final double      MAXIMUM_WIDTH      = 1024;
    private static final double      MAXIMUM_HEIGHT     = 1024;
    private static final Color       DARK_NOISE_COLOR   = Color.rgb(100, 100, 100, 0.10);
    private static final Color       BRIGHT_NOISE_COLOR = Color.rgb(200, 200, 200, 0.05);
    private static final DropShadow  FOREGROUND_SHADOW  = new DropShadow();    
    private static       double      aspectRatio        = PREFERRED_HEIGHT / PREFERRED_WIDTH;
    private static       Text        oneTimeSegment     = new Text("8");
    private static       Text        oneSecondSegment   = new Text("8");
    private double                   width;
    private double                   height;
    private Pane                     pane;
    private Region                   main;
    private ImageView                crystalOverlay;
    private Image                    crystalImage;
    private Rectangle                crystalClip;
    private InnerShadow              mainInnerShadow0;
    private InnerShadow              mainInnerShadow1;
    private Region                   alarm;
    private Text                     timeText;
    private Text                     backgroundTimeText;
    private Text                     secondText;
    private Text                     backgroundSecondText;
    private Text                     title;
    private Text                     dateText;
    private Text                     dayOfWeekText;
    private DateTimeFormatter        dateFormat;
    private double                   valueOffsetRight;
    private double                   digitalFontSizeFactor;
    private Font                     timeFont;
    private Font                     secondFont;
    private Font                     titleFont;
    private Font                     smallFont;
    private StringBuilder            backgroundTextBuilder;
    private Group                    shadowGroup;


    // ******************** Constructors **************************************
    public LcdClockSkin(final LcdClock CONTROL) {
        super(CONTROL);
        aspectRatio = PREFERRED_HEIGHT / PREFERRED_WIDTH;
        valueOffsetRight = 0.0;
        digitalFontSizeFactor = 1.0;
        backgroundTextBuilder = new StringBuilder();
        FOREGROUND_SHADOW.setOffsetX(0);
        FOREGROUND_SHADOW.setOffsetY(1);
        FOREGROUND_SHADOW.setColor(Color.rgb(0, 0, 0, 0.5));
        FOREGROUND_SHADOW.setBlurType(BlurType.TWO_PASS_BOX);
        FOREGROUND_SHADOW.setRadius(2);
        adjustDateFormat();
        init();
        initGraphics();
        registerListeners();
    }


    // ******************** Initialization ************************************
    private void init() {
        if (Double.compare(getSkinnable().getPrefWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getPrefHeight(),
                                                                                      0.0) <= 0 ||
            Double.compare(getSkinnable().getWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getHeight(),
                                                                                  0.0) <= 0) {
            if (getSkinnable().getPrefWidth() > 0 && getSkinnable().getPrefHeight() > 0) {
                getSkinnable().setPrefSize(getSkinnable().getPrefWidth(), getSkinnable().getPrefHeight());
            } else {
                getSkinnable().setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
            }
        }

        if (Double.compare(getSkinnable().getMinWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getMinHeight(),
                                                                                     0.0) <= 0) {
            getSkinnable().setMinSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);
        }

        if (Double.compare(getSkinnable().getMaxWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getMaxHeight(), 0.0) <= 0) {
            getSkinnable().setMaxSize(MAXIMUM_WIDTH, MAXIMUM_HEIGHT);
        }
    }

    private void initGraphics() {
        main = new Region();
        main.getStyleClass().setAll("main");
        main.setOpacity(getSkinnable().isBackgroundVisible() ? 1 : 0);

        mainInnerShadow0 = new InnerShadow();
        mainInnerShadow0.setOffsetX(0.0);
        mainInnerShadow0.setOffsetY(0.0);
        mainInnerShadow0.setRadius(3.0 / 132.0 * PREFERRED_WIDTH);
        mainInnerShadow0.setColor(Color.web("0xffffff80"));
        mainInnerShadow0.setBlurType(BlurType.TWO_PASS_BOX);

        mainInnerShadow1 = new InnerShadow();
        mainInnerShadow1.setOffsetX(0.0);
        mainInnerShadow1.setOffsetY(1.0);
        mainInnerShadow1.setRadius(2.0 / 132.0 * PREFERRED_WIDTH);
        mainInnerShadow1.setColor(Color.web("0x000000a6"));
        mainInnerShadow1.setBlurType(BlurType.TWO_PASS_BOX);
        mainInnerShadow1.setInput(mainInnerShadow0);

        main.setEffect(getSkinnable().isMainInnerShadowVisible() ? mainInnerShadow1 : null);

        crystalClip = new Rectangle(0, 0, PREFERRED_WIDTH, PREFERRED_HEIGHT);
        crystalClip.setArcWidth(5);
        crystalClip.setArcHeight(5);

        crystalImage = createNoiseImage(PREFERRED_WIDTH, PREFERRED_HEIGHT, DARK_NOISE_COLOR, BRIGHT_NOISE_COLOR, 8);
        crystalOverlay = new ImageView(crystalImage);
        crystalOverlay.setClip(this.crystalClip);
        crystalOverlay.setOpacity(getSkinnable().isCrystalOverlayVisible() ? 1 : 0);

        alarm = new Region();
        alarm.getStyleClass().setAll("alarm");
        alarm.setOpacity(getSkinnable().getAlarms().isEmpty() || allAlarmsInactive() ? 0 : 1);

        backgroundTimeText = new Text("");
        backgroundTimeText.getStyleClass().setAll("fg-trsp");
        backgroundTimeText.setOpacity((LcdClock.LcdFont.LCD == getSkinnable().getTimeFont() || LcdClock.LcdFont.ELEKTRA == getSkinnable().getTimeFont()) ? 1 : 0);

        timeText = new Text("");
        timeText.getStyleClass().setAll("fg");

        backgroundSecondText = new Text("");
        backgroundSecondText.getStyleClass().setAll("fg-trsp");
        backgroundSecondText.setOpacity((LcdClock.LcdFont.LCD == getSkinnable().getTimeFont() || LcdClock.LcdFont.ELEKTRA == getSkinnable().getTimeFont()) ? 1 : 0);

        secondText = new Text("");
        secondText.getStyleClass().setAll("fg");

        title = new Text(getSkinnable().getTitle());
        title.getStyleClass().setAll("fg");

        dateText = new Text(getSkinnable().getTime().getMonthValue() + "/" + getSkinnable().getTime().getDayOfMonth() + "/" + getSkinnable().getTime().getYear());
        dateText.getStyleClass().setAll("fg");

        dayOfWeekText = new Text("");
        dayOfWeekText.getStyleClass().setAll("fg");

        shadowGroup = new Group();
        shadowGroup.setEffect(getSkinnable().isForegroundShadowVisible() ? FOREGROUND_SHADOW : null);
        shadowGroup.getChildren().setAll(alarm,
                                         timeText,
                                         secondText,
                                         title,
                                         dateText,
                                         dayOfWeekText);

        pane = new Pane();
        pane.getChildren().setAll(main,
                                  crystalOverlay,
                                  backgroundTimeText,
                                  backgroundSecondText,
                                  shadowGroup);

        getChildren().setAll(pane);

        resize();
        updateLcd();
    }

    private void registerListeners() {
        getSkinnable().widthProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().heightProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        getSkinnable().clockProperty().addListener(observable -> handleControlPropertyChanged("UPDATE"));
        getSkinnable().timeProperty().addListener(observable -> handleControlPropertyChanged("UPDATE"));
        getSkinnable().titleProperty().addListener(observable -> handleControlPropertyChanged("UPDATE"));
        getSkinnable().localeProperty().addListener(observable -> handleControlPropertyChanged("UPDATE"));
        getSkinnable().prefWidthProperty().addListener(observable -> handleControlPropertyChanged("PREF_SIZE"));
        getSkinnable().prefHeightProperty().addListener(observable -> handleControlPropertyChanged("PREF_SIZE"));
        getSkinnable().timeFontProperty().addListener(observable -> handleControlPropertyChanged("FONT"));
        getSkinnable().smallFontProperty().addListener(observable -> handleControlPropertyChanged("FONT"));
        getSkinnable().backgroundVisibleProperty().addListener(observable -> handleControlPropertyChanged("BACKGROUND_VISIBLE"));
        getSkinnable().crystalOverlayVisibleProperty().addListener(observable -> handleControlPropertyChanged("CRYSTAL_OVERLAY_VISIBLE"));
        getSkinnable().mainInnerShadowVisibleProperty().addListener(observable -> handleControlPropertyChanged("MAIN_INNER_SHADOW_VISIBLE"));
        getSkinnable().foregroundShadowVisibleProperty().addListener(observable -> handleControlPropertyChanged("FOREGROUND_SHADOW_VISIBLE"));
        getSkinnable().dateFormatProperty().addListener(observable -> handleControlPropertyChanged("DATE_FORMAT"));
        getSkinnable().dateSeparatorProperty().addListener(observable -> handleControlPropertyChanged("DATE_FORMAT"));
        
        getSkinnable().getAlarms().addListener((ListChangeListener<Alarm>) change -> handleControlPropertyChanged("ALARM"));
        getSkinnable().getStyleClass().addListener((ListChangeListener<String>) change -> {
            resize();
            updateLcd();
        });
    }


    // ******************** Methods *******************************************
    protected void handleControlPropertyChanged(final String PROPERTY) {
        if ("UPDATE".equals(PROPERTY)) {
            updateLcd();
        } else if ("RESIZE".equals(PROPERTY)) {
            resize();
            updateLcd();
        } else if ("PREF_SIZE".equals(PROPERTY)) {
            aspectRatio = getSkinnable().getPrefHeight() / getSkinnable().getPrefWidth();
        } else if ("BACKGROUND_VISIBLE".equals(PROPERTY)) {
            main.setOpacity(getSkinnable().isBackgroundVisible() ? 1 : 0);
            crystalOverlay.setOpacity(getSkinnable().isBackgroundVisible() ? 1 : 0);
        } else if ("CRYSTAL_OVERLAY_VISIBLE".equals(PROPERTY)) {
            crystalOverlay.setOpacity(getSkinnable().isCrystalOverlayVisible() ? 1 : 0);
            resize();
        } else if ("MAIN_INNER_SHADOW_VISIBLE".equals(PROPERTY)) {
            main.setEffect(getSkinnable().isMainInnerShadowVisible() ? mainInnerShadow1 : null);
        } else if ("FOREGROUND_SHADOW_VISIBLE".equals(PROPERTY)) {
            shadowGroup.setEffect(getSkinnable().isForegroundShadowVisible() ? FOREGROUND_SHADOW : null);
        } else if ("FONT".equals(PROPERTY)) {
            updateFonts();
        } else if ("ALARM".equals(PROPERTY)) {
            alarm.setOpacity(getSkinnable().getAlarms().isEmpty() || allAlarmsInactive() ? 0 : 1);
        } else if ("DATE_FORMAT".equals(PROPERTY)) {
            adjustDateFormat();
            updateLcd();
        }
    }
   

    // ******************** Private Methods ***********************************
    private String ensureTwoDigits(final int NUMBER) {
        if (NUMBER < 10) return "0" + NUMBER;
        return Integer.toString(NUMBER);
    }

    private void adjustDateFormat() {
        switch (getSkinnable().getDateFormat()) {
            case DAY_MONTH_YEAR:
                dateFormat = DateTimeFormatter.ofPattern("dd" + getSkinnable().getDateSeparator().chr + "MM" + getSkinnable().getDateSeparator().chr + "yyyy");
                break;
            case YEAR_MONTH_DAY:
            default:
                dateFormat = DateTimeFormatter.ofPattern("yyyy" + getSkinnable().getDateSeparator().chr + "MM" + getSkinnable().getDateSeparator().chr + "dd");
                break;
        }
    }
    
    private boolean allAlarmsInactive() {
        boolean allAlarmsInactive = true;
        for (Alarm alarm : getSkinnable().getAlarms()) {
            if (alarm.isArmed()) {
                allAlarmsInactive = false;
                break;
            }
        }
        return allAlarmsInactive;
    }

    private Image createNoiseImage(final double WIDTH, final double HEIGHT, final Color DARK_COLOR, final Color BRIGHT_COLOR, final double ALPHA_VARIATION_IN_PERCENT) {
        int width  = WIDTH <= 0 ? (int) PREFERRED_WIDTH : (int) WIDTH;
        int height = HEIGHT <= 0 ? (int) PREFERRED_HEIGHT : (int) HEIGHT;
        double alphaVariationInPercent      = getSkinnable().clamp(0, 100, ALPHA_VARIATION_IN_PERCENT);
        final WritableImage IMAGE           = new WritableImage(width, height);
        final PixelWriter   PIXEL_WRITER    = IMAGE.getPixelWriter();
        final Random        BW_RND          = new Random();
        final Random        ALPHA_RND       = new Random();
        final double        ALPHA_START     = alphaVariationInPercent / 100 / 2;
        final double        ALPHA_VARIATION = alphaVariationInPercent / 100;      
        /* This version makes problems when using Robovm
        IntStream.range(0, height).parallel().forEachOrdered(
            y -> {
                IntStream.range(0, width).parallel().forEachOrdered(
                    x -> {
                        final Color  NOISE_COLOR = BW_RND.nextBoolean() == true ? BRIGHT_COLOR : DARK_COLOR;
                        final double NOISE_ALPHA = getSkinnable().clamp(0, 1, ALPHA_START + ALPHA_RND.nextDouble() * ALPHA_VARIATION);
                        PIXEL_WRITER.setColor(x, y, Color.color(NOISE_COLOR.getRed(), NOISE_COLOR.getGreen(), NOISE_COLOR.getBlue(), NOISE_ALPHA));
                    }
                );
            }
        );
        */
        for (int y = 0 ; y < height ; y++) {
            for (int x = 0 ; x < width ; x++) {
                final Color  NOISE_COLOR = BW_RND.nextBoolean() == true ? BRIGHT_COLOR : DARK_COLOR;
                final double NOISE_ALPHA = getSkinnable().clamp(0, 1, ALPHA_START + ALPHA_RND.nextDouble() * ALPHA_VARIATION);
                PIXEL_WRITER.setColor(x, y, Color.color(NOISE_COLOR.getRed(), NOISE_COLOR.getGreen(), NOISE_COLOR.getBlue(), NOISE_ALPHA));
            }
        }
        return IMAGE;
    }

    private void updateFonts() {
        digitalFontSizeFactor = 1.0;
        switch(getSkinnable().getTimeFont()) {
            case LCD:
                digitalFontSizeFactor = 1.05;
                timeFont   = Fonts.digital(0.7 * height);
                secondFont = Fonts.digital(0.2 * height);
                break;
            case DIGITAL:
                digitalFontSizeFactor = 0.7;
                timeFont   = Fonts.digitalReadout(0.7 * height);
                secondFont = Fonts.digitalReadout(0.2 * height);
                break;
            case DIGITAL_BOLD:
                digitalFontSizeFactor = 0.7;
                timeFont   = Fonts.digitalReadoutBold(0.7 * height);
                secondFont = Fonts.digitalReadoutBold(0.2 * height);
                break;
            case ELEKTRA:
                digitalFontSizeFactor = 0.8;
                timeFont   = Fonts.elektra(0.7 * height);
                secondFont = Fonts.elektra(0.186 * height);
                break;
            case STANDARD:
            default:
                digitalFontSizeFactor = 0.7;
                timeFont   = Fonts.robotoMedium(0.6 * height);
                secondFont = Fonts.robotoMedium(0.2 * height);
                break;
        }
        backgroundTimeText.setFont(timeFont);
        backgroundTimeText.setOpacity((LcdClock.LcdFont.LCD == getSkinnable().getTimeFont() ||
                                       LcdClock.LcdFont.DIGITAL == getSkinnable().getTimeFont() ||
                                       LcdClock.LcdFont.DIGITAL_BOLD == getSkinnable().getTimeFont() ||
                                       LcdClock.LcdFont.ELEKTRA == getSkinnable().getTimeFont()) ? 1 : 0);

        backgroundSecondText.setFont(secondFont);
        backgroundSecondText.setOpacity((LcdClock.LcdFont.LCD == getSkinnable().getTimeFont() ||
                                         LcdClock.LcdFont.DIGITAL == getSkinnable().getTimeFont() ||
                                         LcdClock.LcdFont.DIGITAL_BOLD == getSkinnable().getTimeFont() ||
                                         LcdClock.LcdFont.ELEKTRA == getSkinnable().getTimeFont()) ? 1 : 0);

        timeText.setFont(timeFont);
        titleFont = Font.font(getSkinnable().getTitleFont(), FontWeight.NORMAL, (0.15 * height));
        smallFont = Font.font(getSkinnable().getSmallFont(), FontWeight.LIGHT, (0.12 * height));
    }

    private void updateBackgroundText() {
        // Setup the semitransparent background timeText
        backgroundTimeText.setTextOrigin(VPos.BASELINE);
        backgroundTimeText.setTextAlignment(TextAlignment.RIGHT);

        backgroundSecondText.setTextOrigin(VPos.BASELINE);
        backgroundSecondText.setTextAlignment(TextAlignment.RIGHT);

        // Setup the semitransparent background timeText
        // Width of one time segment
        oneTimeSegment.setFont(timeFont);
        if (LcdClock.LcdFont.LCD == getSkinnable().getTimeFont()) {
            oneTimeSegment.setText("8");
        } else if (LcdClock.LcdFont.DIGITAL == getSkinnable().getTimeFont()) {
            oneTimeSegment.setText("_");
        } else if (LcdClock.LcdFont.DIGITAL_BOLD == getSkinnable().getTimeFont()) {
            oneTimeSegment.setText("_");
        } else if (LcdClock.LcdFont.ELEKTRA == getSkinnable().getTimeFont()) {
            oneTimeSegment.setText("_");
        }

        // Width of one time segment
        oneSecondSegment.setFont(secondFont);
        if (LcdClock.LcdFont.LCD == getSkinnable().getTimeFont()) {
            oneSecondSegment.setText("8");
        } else if (LcdClock.LcdFont.DIGITAL == getSkinnable().getTimeFont()) {
            oneSecondSegment.setText("_");
        } else if (LcdClock.LcdFont.DIGITAL_BOLD == getSkinnable().getTimeFont()) {
            oneSecondSegment.setText("_");
        } else if (LcdClock.LcdFont.ELEKTRA == getSkinnable().getTimeFont()) {
            oneSecondSegment.setText("_");
        }

        // Add segments to background timeText
        backgroundTextBuilder.setLength(0);
        backgroundTextBuilder.append(oneTimeSegment.getText());
        backgroundTextBuilder.append(oneTimeSegment.getText());
        backgroundTextBuilder.append(":");
        backgroundTextBuilder.append(oneTimeSegment.getText());
        backgroundTextBuilder.append(oneTimeSegment.getText());

        backgroundTimeText.setText(backgroundTextBuilder.toString());

        backgroundSecondText.setText("88");
    }

    private void updateLcd() {
        timeText.setText(ensureTwoDigits(getSkinnable().getTime().getHour()) + ":" + ensureTwoDigits(getSkinnable().getTime().getMinute()));
        secondText.setText(ensureTwoDigits(getSkinnable().getTime().getSecond()));
        updateBackgroundText();

        // Visualize the lcd semitransparent background timeText
        backgroundTimeText.setX(width - 2 - backgroundTimeText.getLayoutBounds().getWidth() - valueOffsetRight);
        backgroundTimeText.setY(height - (backgroundTimeText.getLayoutBounds().getHeight() * digitalFontSizeFactor) * 0.5);

        timeText.setX((width - 2 - timeText.getLayoutBounds().getWidth()) - valueOffsetRight);

        // Update the title
        title.setText(getSkinnable().getTitle());
        title.setX((width - title.getLayoutBounds().getWidth()) * 0.5);

        // Update the lower center timeText
        dayOfWeekText.setText(getSkinnable().getTime().getDayOfWeek().getDisplayName(TextStyle.FULL_STANDALONE, getSkinnable().getLocale()));
        dayOfWeekText.setX(0.0416666667 * height);

        // Update the lower right timeText        
        dateText.setText(dateFormat.format(getSkinnable().getTime()));        
        dateText.setX(width - dateText.getLayoutBounds().getWidth() - 0.0416666667 * height);
        dateText.setY(main.getLayoutY() + height - 3 - 0.0416666667 * height);
    }

    private void resize() {
        width  = getSkinnable().getWidth();
        height = getSkinnable().getHeight();
        if (getSkinnable().isKeepAspect()) {
            if (aspectRatio * width > height) {
                width = 1 / (aspectRatio / height);
            } else if (1 / (aspectRatio / height) > width) {
                height = aspectRatio * width;
            }
        }

        if (width > 0 && height > 0) {
            pane.setMaxSize(width, height);
            
            main.setPrefSize(width, height);
            mainInnerShadow0.setRadius(3.0 / 132.0 * height);
            mainInnerShadow1.setRadius(2.0 / 132.0 * height);

            if (crystalOverlay.isVisible()) {
                crystalClip.setWidth(width);
                crystalClip.setHeight(height);
                crystalOverlay.setImage(createNoiseImage(width, height, DARK_NOISE_COLOR, BRIGHT_NOISE_COLOR, 8));
                crystalOverlay.setCache(true);
            }

            alarm.setPrefSize(0.15 * height, 0.15 * height);
            alarm.setTranslateX(width - alarm.getPrefWidth() - height * 0.05);
            alarm.setTranslateY(height * 0.42 - alarm.getPrefHeight());

            updateFonts();

            // Setup the lcd unit
            secondText.setFont(secondFont);
            secondText.setTextOrigin(VPos.BASELINE);
            secondText.setTextAlignment(TextAlignment.RIGHT);

            secondText.setX((width - secondText.getLayoutBounds().getWidth()) - height * 0.04);
            secondText.setY(height - (timeText.getLayoutBounds().getHeight() * digitalFontSizeFactor) * 0.5);
            valueOffsetRight = (secondText.getLayoutBounds().getWidth() + height * 0.0833333333); // distance between value and unit

            timeText.setX(width - 2 - timeText.getLayoutBounds().getWidth() - valueOffsetRight);
            timeText.setY(height - (timeText.getLayoutBounds().getHeight() * digitalFontSizeFactor) * 0.5);

            // Visualize the lcd semitransparent background timeText
            updateBackgroundText();

            backgroundTimeText.setX(width - 2 - backgroundTimeText.getLayoutBounds().getWidth() - valueOffsetRight);
            backgroundTimeText.setY(height - (backgroundTimeText.getLayoutBounds().getHeight() * digitalFontSizeFactor) * 0.5);

            backgroundSecondText.setX((width - secondText.getLayoutBounds().getWidth()) - height * 0.04);
            backgroundSecondText.setY(height - (timeText.getLayoutBounds().getHeight() * digitalFontSizeFactor) * 0.5);

            // Setup the font for the lcd title, number system, min measured, max measure and former value
            // Title
            title.setFont(titleFont);
            title.setTextOrigin(VPos.BASELINE);
            title.setTextAlignment(TextAlignment.CENTER);
            title.setText(getSkinnable().getTime().getDayOfWeek().name());
            title.setX((width - title.getLayoutBounds().getWidth()) * 0.5);
            title.setY(main.getLayoutY() + title.getLayoutBounds().getHeight() - 0.04 * height + 2);

            // Date
            dateText.setFont(smallFont);
            dateText.setTextOrigin(VPos.BASELINE);
            dateText.setTextAlignment(TextAlignment.RIGHT);
            dateText.setText(getSkinnable().getTime().getMonthValue() + "/" + getSkinnable().getTime().getDayOfMonth() + "/" + getSkinnable().getTime().getYear());
            dateText.setX(main.getLayoutX() + (width - dateText.getLayoutBounds().getWidth()) * 0.5);
            dateText.setY(main.getLayoutY() + height - 3 - 0.0416666667 * height);

            // Day of week
            dayOfWeekText.setFont(smallFont);
            dayOfWeekText.setTextOrigin(VPos.BASELINE);
            dayOfWeekText.setTextAlignment(TextAlignment.LEFT);
            dayOfWeekText.setX(0.0416666667 * height);
            dayOfWeekText.setY(main.getLayoutY() + height - 3 - 0.0416666667 * height);
        }
    }
}
