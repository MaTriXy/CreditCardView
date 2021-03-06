/*
 * Copyright (C) 2015 Vinay Gaba
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vinaygaba.creditcardview;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vinaygaba.creditcardview.util.AndroidUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Pattern;

import static com.vinaygaba.creditcardview.CardNumberFormat.ALL_DIGITS;
import static com.vinaygaba.creditcardview.CardNumberFormat.MASKED_ALL;
import static com.vinaygaba.creditcardview.CardNumberFormat.MASKED_ALL_BUT_LAST_FOUR;
import static com.vinaygaba.creditcardview.CardNumberFormat.ONLY_LAST_FOUR;
import static com.vinaygaba.creditcardview.CardType.AMERICAN_EXPRESS;
import static com.vinaygaba.creditcardview.CardType.AUTO;
import static com.vinaygaba.creditcardview.CardType.DISCOVER;
import static com.vinaygaba.creditcardview.CardType.MASTERCARD;
import static com.vinaygaba.creditcardview.CardType.PATTERN_AMERICAN_EXPRESS;
import static com.vinaygaba.creditcardview.CardType.PATTERN_DISCOVER;
import static com.vinaygaba.creditcardview.CardType.PATTERN_MASTER_CARD;
import static com.vinaygaba.creditcardview.CardType.VISA;

@SuppressLint("DefaultLocale")
public class CreditCardView extends RelativeLayout {

    @IntDef({VISA, MASTERCARD, AMERICAN_EXPRESS, DISCOVER, AUTO})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CreditCardType {
    }

    @IntDef({ALL_DIGITS, MASKED_ALL_BUT_LAST_FOUR, ONLY_LAST_FOUR, MASKED_ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CreditCardFormat {
    }

    private static int CARD_FRONT = 0;
    private static int CARD_BACK = 1;
    private static final boolean DEBUG = false;
    private Context mContext;
    private String mCardNumber = "";
    private String mCardName = "";
    private String mExpiryDate = "";
    private String mCvv = "";
    private int mCardNumberTextColor = Color.WHITE;
    private int mCardNumberFormat = ALL_DIGITS;
    private int mCardNameTextColor = Color.WHITE;
    private int mExpiryDateTextColor = Color.WHITE;
    private int mCvvTextColor = Color.BLACK;
    private int mValidTillTextColor = Color.WHITE;
    private int mType = VISA;
    private int mBrandLogo;
    private int cardSide = CARD_FRONT;
    private boolean mPutChip = false;
    private boolean mIsEditable = false;
    private boolean mIsCardNumberEditable = false;
    private boolean mIsCardNameEditable = false;
    private boolean mIsExpiryDateEditable = false;
    private boolean mIsCvvEditable = false;
    private int mHintTextColor = Color.WHITE;
    private int mCvvHintColor = Color.WHITE;
    private int mCardFrontBackground;
    private int mCardBackBackground;
    private boolean mIsFlippable = false;
    private Typeface creditCardTypeFace;
    private ImageButton mFlipBtn;
    private EditText cardNumber;
    private EditText cardName;
    private EditText expiryDate;
    private EditText cvv;
    private TextView validTill;
    private ImageView type;
    private ImageView brandLogo;
    private ImageView chip;
    private View stripe, authorized_sig_tv, signature;

    public CreditCardView(Context context) {
        this(context, null);
    }

    public CreditCardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (context != null) {
            this.mContext = context;
        } else {
            this.mContext = getContext();
        }

        init();
        loadAttributes(attrs);
        initDefaults();
        addListeners();
    }

    /**
     * Initialize various views and variables
     */
    private void init() {
        final LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.creditcardview, this, true);

        // Added this check to fix the issue of custom view not rendering correctly in the layout
        // preview.
        if (!isInEditMode()) {
            // Font path
            final String fontPath = mContext.getString(R.string.font_path);
            // Loading Font Face
            creditCardTypeFace = Typeface.createFromAsset(mContext.getAssets(), fontPath);
        }

        cardNumber = (EditText) findViewById(R.id.card_number);
        cardName = (EditText) findViewById(R.id.card_name);
        type = (ImageView) findViewById(R.id.card_logo);
        brandLogo = (ImageView) findViewById(R.id.brand_logo);
        chip = (ImageView) findViewById(R.id.chip);
        validTill = (TextView) findViewById(R.id.valid_till);
        expiryDate = (EditText) findViewById(R.id.expiry_date);
        mFlipBtn = (ImageButton)findViewById(R.id.flip_btn);
        stripe = findViewById(R.id.stripe);
        authorized_sig_tv = findViewById(R.id.authorized_sig_tv);
        signature = findViewById(R.id.signature);
        cvv = (EditText)findViewById(R.id.cvv_et);
    }

    private void loadAttributes(@Nullable AttributeSet attrs) {

        final TypedArray a = mContext.getTheme().obtainStyledAttributes(attrs,
                R.styleable.CreditCardView, 0, 0);

        try {
            mCardNumber = a.getString(R.styleable.CreditCardView_cardNumber);
            mCardName = a.getString(R.styleable.CreditCardView_cardName);
            mExpiryDate = a.getString(R.styleable.CreditCardView_expiryDate);
            mCardNumberTextColor = a.getColor(R.styleable.CreditCardView_cardNumberTextColor,
                    Color.WHITE);
            mCardNumberFormat = a.getInt(R.styleable.CreditCardView_cardNumberFormat, 0);
            mCardNameTextColor = a.getColor(R.styleable.CreditCardView_cardNumberTextColor,
                    Color.WHITE);
            mExpiryDateTextColor = a.getColor(R.styleable.CreditCardView_expiryDateTextColor,
                    Color.WHITE);
            mCvvTextColor = a.getColor(R.styleable.CreditCardView_cvvTextColor,
                    Color.BLACK);
            mValidTillTextColor = a.getColor(R.styleable.CreditCardView_validTillTextColor,
                    Color.WHITE);
            mType = a.getInt(R.styleable.CreditCardView_type, 0);
            mBrandLogo = a.getResourceId(R.styleable.CreditCardView_brandLogo, 0);
            // mBrandLogoPosition = a.getInt(R.styleable.CreditCardView_brandLogoPosition, 1);
            mPutChip = a.getBoolean(R.styleable.CreditCardView_putChip, false);
            mIsEditable = a.getBoolean(R.styleable.CreditCardView_isEditable, false);
            //For more granular control to the fields. Issue #7
            mIsCardNameEditable = a.getBoolean(R.styleable.CreditCardView_isCardNameEditable, mIsEditable);
            mIsCardNumberEditable = a.getBoolean(R.styleable.CreditCardView_isCardNumberEditable, mIsEditable);
            mIsExpiryDateEditable = a.getBoolean(R.styleable.CreditCardView_isExpiryDateEditable, mIsEditable);
            mIsCvvEditable = a.getBoolean(R.styleable.CreditCardView_isCvvEditable, mIsEditable);
            mHintTextColor = a.getColor(R.styleable.CreditCardView_hintTextColor, Color.WHITE);
            mIsFlippable = a.getBoolean(R.styleable.CreditCardView_isFlippable, mIsFlippable);
            mCvv = a.getString(R.styleable.CreditCardView_cvv);
            mCardBackBackground = a.getResourceId(R.styleable.CreditCardView_cardBackBackground, R.drawable.cardbackground_canvas);

        } finally {
            a.recycle();
        }
    }

    private void initDefaults() {

        // Set default background if background attribute was not entered in the xml
        if (getBackground() == null) {
            mCardFrontBackground = R.drawable.cardbackground_sky;
            setBackgroundResource(mCardFrontBackground);
        }



        if (!mIsEditable) {
            // If card is not set to be editable, disable the edit texts
            cardNumber.setEnabled(false);
            cardName.setEnabled(false);
            expiryDate.setEnabled(false);
            cvv.setEnabled(false);
        } else {
            // If the card is editable, set the hint text and hint values which will be displayed
            // when the edit text is blank
            cardNumber.setHint(R.string.card_number_hint);
            cardNumber.setHintTextColor(mHintTextColor);

            cardName.setHint(R.string.card_name_hint);
            cardName.setHintTextColor(mHintTextColor);

            expiryDate.setHint(R.string.expiry_date_hint);
            expiryDate.setHintTextColor(mHintTextColor);

            cvv.setHint(R.string.cvv_hint);
            cvv.setHintTextColor(mCvvTextColor);
        }

        //For more granular control of the editable fields. Issue #7
        if(mIsCardNameEditable!=mIsEditable){
            //If the mIsCardNameEditable is different than mIsEditable field, the granular
            //precedence comes into picture and the value needs to be checked and modified
            //accordingly
            if(mIsCardNameEditable){
                cardName.setHint(R.string.card_name_hint);
                cardName.setHintTextColor(mHintTextColor);
            }
            else{
                cardName.setHint("");
            }

            cardName.setEnabled(mIsCardNameEditable);
        }

        if(mIsCardNumberEditable!=mIsEditable){
            //If the mIsCardNumberEditable is different than mIsEditable field, the granular
            //precedence comes into picture and the value needs to be checked and modified
            //accordingly
            if(mIsCardNumberEditable){
                cardNumber.setHint(R.string.card_number_hint);
                cardNumber.setHintTextColor(mHintTextColor);
            }
            else{
                cardNumber.setHint("");
            }
            cardNumber.setEnabled(mIsCardNumberEditable);
        }

        if(mIsExpiryDateEditable!=mIsEditable){
            //If the mIsExpiryDateEditable is different than mIsEditable field, the granular
            //precedence comes into picture and the value needs to be checked and modified
            //accordingly
            if(mIsExpiryDateEditable){
                expiryDate.setHint(R.string.expiry_date_hint);
                expiryDate.setHintTextColor(mHintTextColor);
            }
            else{
                expiryDate.setHint("");
            }
            expiryDate.setEnabled(mIsExpiryDateEditable);
        }

        // If card number is not null, add space every 4 characters and format it in the appropriate
        // format
        if (mCardNumber != null) {
            cardNumber.setText(checkCardNumberFormat(addSpaceToCardNumber(mCardNumber)));
        }

        // Set the user entered card number color to card number field
        cardNumber.setTextColor(mCardNumberTextColor);

        // Added this check to fix the issue of custom view not rendering correctly in the layout
        // preview.
        if (!isInEditMode()) {
            cardNumber.setTypeface(creditCardTypeFace);
        }

        // If card name is not null, convert the text to upper case
        if (mCardName != null) {
            cardName.setText(mCardName.toUpperCase());
        }

        // This filter will ensure the text entered is in uppercase when the user manually enters
        // the card name
        cardName.setFilters(new InputFilter[]{
                new InputFilter.AllCaps()
        });

        // Set the user entered card name color to card name field
        cardName.setTextColor(mCardNumberTextColor);

        // Added this check to fix the issue of custom view not rendering correctly in the layout
        // preview.
        if (!isInEditMode()) {
            cardName.setTypeface(creditCardTypeFace);
        }

        // Set the appropriate logo based on the type of card
        type.setBackgroundResource(getLogo(mType));

        // If background logo attribute is present, set it as the brand logo background resource
        if (mBrandLogo != 0) {
            brandLogo.setBackgroundResource(mBrandLogo);
            // brandLogo.setLayoutParams(params);
        }

        // If putChip attribute is present, change the visibility of the putChip view and display it
        if (mPutChip) {
            chip.setVisibility(View.VISIBLE);
        }

        // If expiry date is not null, set it to the expiryDate TextView
        if (mExpiryDate != null) {
            expiryDate.setText(mExpiryDate);
        }

        // Set the user entered expiry date color to expiry date field
        expiryDate.setTextColor(mExpiryDateTextColor);

        // Added this check to fix the issue of custom view not rendering correctly in the layout
        // preview.
        if (!isInEditMode()) {
            expiryDate.setTypeface(creditCardTypeFace);
        }

        // Set the appropriate text color to the validTill TextView
        validTill.setTextColor(mValidTillTextColor);

        // If CVV is not null, set it to the expiryDate TextView
        if (mCvv != null) {
            cvv.setText(mCvv);
        }

        // Set the user entered card number color to card number field
        cvv.setTextColor(mCvvTextColor);

        // Added this check to fix the issue of custom view not rendering correctly in the layout
        // preview.
        if (!isInEditMode()) {
            cvv.setTypeface(creditCardTypeFace);
        }

        if(mIsCvvEditable != mIsEditable){

            if(mIsCvvEditable){
                cvv.setHint(R.string.cvv_hint);
                cvv.setHintTextColor(mCvvHintColor);
            } else {
                cvv.setHint("");
            }

            cvv.setEnabled(mIsCvvEditable);

        }

        if(mIsFlippable){
            mFlipBtn.setVisibility(View.VISIBLE);
        }
        mFlipBtn.setEnabled(mIsFlippable);

    }
    private void addListeners() {

        // Add text change listener
        cardNumber.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Change card type to auto to dynamically detect the card type based on the card
                // number
                mType = AUTO;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Delete any spaces the user might have entered manually. The library automatically
                // adds spaces after every 4 characters to the view.
                mCardNumber = s.toString().replaceAll("\\s+", "");
            }
        });

        // Add focus change listener to detect focus being shifted from the cardNumber EditText
        cardNumber.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // If the field just lost focus
                if (!hasFocus) {
                    //Fix for NPE. Issue #6
                    if(mCardNumber != null) {
                        if (mCardNumber.length() > 12) {
                            // If the length of card is >12, add space every 4 characters and format it
                            // in the appropriate format
                            cardNumber
                                    .setText(checkCardNumberFormat(addSpaceToCardNumber(mCardNumber)));

                            // If card type is "auto",find the appropriate logo
                            if (mType == AUTO) {
                                type.setBackgroundResource(getLogo(mType));
                            }
                        }
                    }
                }
            }
        });

        cardName.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Set the mCardName attribute the user entered value in the Card Name field
                mCardName = s.toString().toUpperCase();
            }
        });

        expiryDate.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Set the mExpiryDate attribute the user entered value in the Expiry Date field
                mExpiryDate = s.toString();
            }
        });

        mFlipBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                flip();
            }
        });
    }

    public boolean isFlippable(){
        return mIsFlippable;
    }

    public void setIsFlippable(boolean flippable){
        mIsFlippable = flippable;
        if(mIsFlippable){
            mFlipBtn.setVisibility(View.VISIBLE);
        } else {
            mFlipBtn.setVisibility(View.INVISIBLE);
        }
        mFlipBtn.setEnabled(mIsFlippable);
    }

    public void flip(){
        if(mIsFlippable){
            if(AndroidUtils.icsOrBetter()){
                if(cardSide == CARD_FRONT){
                    rotateInToBack();
                } else if(cardSide == CARD_BACK){
                    rotateInToFront();
                }
            } else {
                if(cardSide == CARD_FRONT){
                    rotateInToBackBeforeEleven();
                } else if(cardSide == CARD_BACK){
                    rotateInToFrontBeforeEleven();
                }
            }

        }
    }

    private void showFrontView(){
        cardNumber.setVisibility(View.VISIBLE);
        cardName.setVisibility(View.VISIBLE);
        type.setVisibility(View.VISIBLE);
        brandLogo.setVisibility(View.VISIBLE);
        if(mPutChip) {
            chip.setVisibility(View.VISIBLE);
        }
        validTill.setVisibility(View.VISIBLE);
        expiryDate.setVisibility(View.VISIBLE);
    }

    private void hideFrontView(){
        cardNumber.setVisibility(View.GONE);
        cardName.setVisibility(View.GONE);
        type.setVisibility(View.GONE);
        brandLogo.setVisibility(View.GONE);
        chip.setVisibility(View.GONE);
        validTill.setVisibility(View.GONE);
        expiryDate.setVisibility(View.GONE);
    }

    private void showBackView(){
        stripe.setVisibility(View.VISIBLE);
        authorized_sig_tv.setVisibility(View.VISIBLE);
        signature.setVisibility(View.VISIBLE);
        cvv.setVisibility(View.VISIBLE);
    }

    private void hideBackView(){
        stripe.setVisibility(View.GONE);
        authorized_sig_tv.setVisibility(View.GONE);
        signature.setVisibility(View.GONE);
        cvv.setVisibility(View.GONE);
    }

    private void redrawViews() {
        invalidate();
        requestLayout();
    }

    public String getCardNumber() {
        return mCardNumber;
    }

    public void setCardNumber(String cardNumber) {
        mCardNumber = cardNumber.replaceAll("\\s+", "");
        this.cardNumber.setText(addSpaceToCardNumber(mCardNumber));
        redrawViews();
    }

    public String getCardName() {
        return mCardName;
    }

    public void setCardName(String cardName) {
        mCardName = cardName.toUpperCase();
        this.cardName.setText(mCardName);
        redrawViews();
    }

    @ColorInt
    public int getCardNumberTextColor() {
        return mCardNumberTextColor;
    }

    public void setCardNumberTextColor(@ColorInt int cardNumberTextColor) {
        mCardNumberTextColor = cardNumberTextColor;
        this.cardNumber.setTextColor(mCardNumberTextColor);
        redrawViews();
    }

    @CreditCardFormat
    public int getCardNumberFormat() {
        return mCardNumberFormat;
    }

    public void setCardNumberFormat(@CreditCardFormat int cardNumberFormat) {
        if (cardNumberFormat < 0 | cardNumberFormat > 3) {
            throw new UnsupportedOperationException("CardNumberFormat: " + cardNumberFormat + "  " +
                    "is not supported. Use `CardNumberFormat.*` or `CardType.ALL_DIGITS` if " +
                    "unknown");
        }
        mCardNumberFormat = cardNumberFormat;
        this.cardNumber.setText(checkCardNumberFormat(mCardNumber));
        redrawViews();
    }

    @ColorInt
    public int getCardNameTextColor() {
        return mCardNameTextColor;
    }

    public void setCardNameTextColor(@ColorInt int cardNameTextColor) {
        mCardNameTextColor = cardNameTextColor;
        this.cardName.setTextColor(mCardNameTextColor);
        redrawViews();
    }

    public String getExpiryDate() {
        return mExpiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        mExpiryDate = expiryDate;
        this.expiryDate.setText(mExpiryDate);
        redrawViews();
    }

    @ColorInt
    public int getExpiryDateTextColor() {
        return mExpiryDateTextColor;
    }

    public void setExpiryDateTextColor(@ColorInt int expiryDateTextColor) {
        mExpiryDateTextColor = expiryDateTextColor;
        this.expiryDate.setTextColor(mExpiryDateTextColor);
        redrawViews();
    }

    @ColorInt
    public int getValidTillTextColor() {
        return mValidTillTextColor;
    }

    public void setValidTillTextColor(@ColorInt int validTillTextColor) {
        mValidTillTextColor = validTillTextColor;
        this.validTill.setTextColor(mValidTillTextColor);
        redrawViews();
    }

    @CreditCardType
    public int getType() {
        return mType;
    }

    public void setType(@CreditCardType int type) {
        if (type < 0 | type > 4) {
            throw new UnsupportedOperationException("CardType: " + type + "  is not supported. " +
                    "Use `CardType.*` or `CardType.AUTO` if unknown");
        }
        mType = type;
        this.type.setBackgroundResource(getLogo(mType));
        redrawViews();
    }

    public boolean getIsEditable() {
        return mIsEditable;
    }

    public void setIsEditable(boolean isEditable) {
        mIsEditable = isEditable;
        redrawViews();
    }

    public boolean getIsCardNameEditable() {
        return mIsCardNameEditable;
    }


    public void setIsCardNameEditable(boolean isCardNameEditable) {
        mIsCardNameEditable = isCardNameEditable;
        redrawViews();
    }

    public boolean getIsCardNumberEditable() {
        return mIsCardNumberEditable;
    }

    public void setIsCardNumberEditable(boolean isCardNumberEditable) {
        mIsCardNumberEditable = isCardNumberEditable;
        redrawViews();
    }

    public boolean getIsExpiryDateEditable() {
        return mIsExpiryDateEditable;
    }

    public void setIsExpiryDateEditable(boolean isExpiryDateEditable) {
        mIsExpiryDateEditable = isExpiryDateEditable;
        redrawViews();
    }

    @ColorInt
    public int getHintTextColor() {
        return mHintTextColor;
    }

    public void setHintTextColor(@ColorInt int hintTextColor) {
        mHintTextColor = hintTextColor;
        this.cardName.setHintTextColor(mHintTextColor);
        this.cardNumber.setHintTextColor(mHintTextColor);
        this.expiryDate.setHintTextColor(mHintTextColor);

        redrawViews();
    }

    @DrawableRes
    public int getBrandLogo() {
        return mBrandLogo;
    }

    public void setBrandLogo(@DrawableRes int brandLogo) {
        mBrandLogo = brandLogo;
        this.brandLogo.setBackgroundResource(mBrandLogo);
        redrawViews();
    }

    public int getBrandLogoPosition() {
        return mBrandLogo;
    }

    public void setBrandLogoPosition(int brandLogoPosition) {
        redrawViews();
    }

    public void putChip(boolean flag) {
        mPutChip = flag;
        chip.setVisibility(mPutChip?View.VISIBLE:View.GONE);
        redrawViews();
    }

    public void setIsCvvEditable(boolean editable){
        mIsCvvEditable =editable;
        redrawViews();
    }

    public boolean getIsCvvEditable(){
        return mIsCvvEditable;
    }

    @DrawableRes
    public int getCardBackBackground() {
        return mCardBackBackground;
    }

    public void setCardBackBackground(@DrawableRes int cardBackBackground) {
            mCardBackBackground = cardBackBackground;
            setBackgroundResource(mCardBackBackground);
        redrawViews();
    }

    /**
     * Return the appropriate drawable resource based on the card type
     *
     * @param type type of card.
     */
    @DrawableRes
    private int getLogo(@CreditCardType int type) {

        switch (type) {
            case VISA:
                return R.drawable.visa;

            case MASTERCARD:
                return R.drawable.mastercard;

            case AMERICAN_EXPRESS:
                return R.drawable.amex;

            case DISCOVER:
                return R.drawable.discover;

            case AUTO:
                return findCardType();

            default:
                throw new UnsupportedOperationException("CardType: " + type + "  is not supported" +
                        ". Use `CardType.*` or `CardType.AUTO` if unknown");
        }

    }

    /**
     * Returns the formatted card number based on the user entered value for card number format
     *
     * @param cardNumber Card Number.
     */
    private String checkCardNumberFormat(String cardNumber) {

        if (DEBUG) {
            Log.e("Card Number", cardNumber);
        }

        switch (getCardNumberFormat()) {
            case MASKED_ALL_BUT_LAST_FOUR:
                cardNumber = "**** **** **** "
                        + cardNumber.substring(cardNumber.length() - 4, cardNumber.length());
                break;
            case ONLY_LAST_FOUR:
                cardNumber = cardNumber.substring(cardNumber.length() - 4, cardNumber.length());
                break;
            case MASKED_ALL:
                cardNumber = "**** **** **** ****";
                break;
            default:
                //do nothing.
                break;
        }
        return cardNumber;
    }

    /**
     * Returns the appropriate card type drawable resource based on the regex pattern of the card
     * number
     */
    @DrawableRes
    private int findCardType() {

        int type = VISA;
        if (cardNumber.length() > 0) {

            final String cardNumber = getCardNumber().replaceAll("\\s+", "");

            if (Pattern.compile(PATTERN_MASTER_CARD).matcher(cardNumber).matches()) {
                type = MASTERCARD;
            } else if (Pattern.compile(PATTERN_AMERICAN_EXPRESS).matcher(cardNumber)
                    .matches()) {
                type = AMERICAN_EXPRESS;
            } else if (Pattern.compile(PATTERN_DISCOVER).matcher(cardNumber).matches()) {
                type = DISCOVER;
            }
        }
        setType(type);

        return getLogo(type);
    }

    /**
     * Adds space after every 4 characters to the card number if the card number is divisible by 4
     *
     * @param cardNumber Card Number.
     */
    private String addSpaceToCardNumber(String cardNumber) {

        if (cardNumber.length() % 4 != 0) {
            return cardNumber;
        } else {
            final StringBuilder result = new StringBuilder();
            for (int i = 0; i < cardNumber.length(); i++) {
                if (i % 4 == 0 && i != 0 && i != cardNumber.length() - 1) {
                    result.append(" ");
                }
                result.append(cardNumber.charAt(i));
            }
            return result.toString();
        }
    }

    @TargetApi(11)
    private void rotateInToBack(){
        AnimatorSet set = new AnimatorSet();
        final ObjectAnimator rotateIn = ObjectAnimator.ofFloat(this, "rotationY", 0, 90);
        final ObjectAnimator hideFrontView = ObjectAnimator.ofFloat(this, "alpha", 1, 0);
        rotateIn.setInterpolator(new AccelerateDecelerateInterpolator());
        rotateIn.setDuration(300);
        hideFrontView.setDuration(1);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                rotateOutToBack();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        set.play(hideFrontView).after(rotateIn);
        set.start();
    }

    @TargetApi(11)
    private void rotateInToFront(){
        AnimatorSet set = new AnimatorSet();
        final ObjectAnimator rotateIn = ObjectAnimator.ofFloat(this, "rotationY", 0, 90);
        final ObjectAnimator hideBackView = ObjectAnimator.ofFloat(this, "alpha", 1, 0);
        rotateIn.setDuration(300);
        hideBackView.setDuration(1);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                rotateOutToFront();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        set.play(hideBackView).after(rotateIn);
        set.start();
    }

    @TargetApi(11)
    private void rotateOutToBack(){
        hideFrontView();
        showBackView();
        CreditCardView.this.setRotationY(-90);
        setBackgroundResource(mCardBackBackground);
        AnimatorSet set = new AnimatorSet();
        final ObjectAnimator flipView = ObjectAnimator.ofInt(CreditCardView.this, "rotationY", 90, -90);
        final ObjectAnimator rotateOut = ObjectAnimator.ofFloat(CreditCardView.this, "rotationY", -90, 0);
        final ObjectAnimator showBackView = ObjectAnimator.ofFloat(CreditCardView.this, "alpha", 0, 1);
        flipView.setDuration(0);
        showBackView.setDuration(1);
        rotateOut.setDuration(300);
        showBackView.setStartDelay(150);
        rotateOut.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                //Do nothing
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                cardSide = CARD_BACK;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                //Do nothing
            }
        });
        set.play(flipView).with(showBackView).before(rotateOut);
        set.start();
    }

    @TargetApi(11)
    private void rotateOutToFront(){
        showFrontView();
        hideBackView();
        CreditCardView.this.setRotationY(-90);
        setBackgroundResource(R.drawable.cardbackground_sky);
        AnimatorSet set = new AnimatorSet();
        final ObjectAnimator flipView = ObjectAnimator.ofInt(CreditCardView.this, "rotationY", 90, -90);
        final ObjectAnimator rotateOut = ObjectAnimator.ofFloat(CreditCardView.this, "rotationY", -90, 0);
        final ObjectAnimator showFrontView = ObjectAnimator.ofFloat(CreditCardView.this, "alpha", 0, 1);
        showFrontView.setDuration(1);
        rotateOut.setDuration(300);
        showFrontView.setStartDelay(150);
        rotateOut.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                //Do nothing
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                cardSide = CARD_FRONT;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                //Do nothing
            }
        });
        set.play(flipView).with(showFrontView).before(rotateOut);
        set.start();
    }

    private void rotateInToBackBeforeEleven(){
        com.nineoldandroids.animation.AnimatorSet set = new com.nineoldandroids.animation.AnimatorSet();
        final com.nineoldandroids.animation.ObjectAnimator rotateIn = com.nineoldandroids.animation.ObjectAnimator.ofFloat(this, "rotationY", 0, 90);
        final com.nineoldandroids.animation.ObjectAnimator hideFrontView = com.nineoldandroids.animation.ObjectAnimator.ofFloat(this, "alpha", 1, 0);
        rotateIn.setInterpolator(new AccelerateDecelerateInterpolator());
        rotateIn.setDuration(300);
        hideFrontView.setDuration(1);
        set.addListener(new com.nineoldandroids.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(com.nineoldandroids.animation.Animator animation) {

            }

            @Override
            public void onAnimationEnd(com.nineoldandroids.animation.Animator animation) {
                rotateOutToBackBeforeEleven();
            }

            @Override
            public void onAnimationCancel(com.nineoldandroids.animation.Animator animation) {

            }

            @Override
            public void onAnimationRepeat(com.nineoldandroids.animation.Animator animation) {

            }
        });
        set.play(hideFrontView).after(rotateIn);
        set.start();
    }

    private void rotateInToFrontBeforeEleven(){
        com.nineoldandroids.animation.AnimatorSet set = new com.nineoldandroids.animation.AnimatorSet();
        final com.nineoldandroids.animation.ObjectAnimator rotateIn = com.nineoldandroids.animation.ObjectAnimator.ofFloat(this, "rotationY", 0, 90);
        final com.nineoldandroids.animation.ObjectAnimator hideBackView = com.nineoldandroids.animation.ObjectAnimator.ofFloat(this, "alpha", 1, 0);
        rotateIn.setInterpolator( new AccelerateDecelerateInterpolator());
        rotateIn.setDuration(300);
        hideBackView.setDuration(1);
        set.addListener(new com.nineoldandroids.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(com.nineoldandroids.animation.Animator animation) {

            }

            @Override
            public void onAnimationEnd(com.nineoldandroids.animation.Animator animation) {
                rotateOutToFrontBeforeEleven();
            }

            @Override
            public void onAnimationCancel(com.nineoldandroids.animation.Animator animation) {

            }

            @Override
            public void onAnimationRepeat(com.nineoldandroids.animation.Animator animation) {

            }
        });
        set.play(hideBackView).after(rotateIn);
        set.start();
    }

    private void rotateOutToBackBeforeEleven(){
        hideFrontView();
        showBackView();
        setBackgroundResource(mCardBackBackground);
        com.nineoldandroids.animation.AnimatorSet set = new com.nineoldandroids.animation.AnimatorSet();
        com.nineoldandroids.animation.ObjectAnimator flip = com.nineoldandroids.animation.ObjectAnimator.ofFloat(CreditCardView.this, "rotationY", 90, -90);
        com.nineoldandroids.animation.ObjectAnimator rotateOut = com.nineoldandroids.animation.ObjectAnimator.ofFloat(CreditCardView.this, "rotationY", -90, 0);
        com.nineoldandroids.animation.ObjectAnimator showBackView = com.nineoldandroids.animation.ObjectAnimator.ofFloat(CreditCardView.this, "alpha", 0, 1);
        flip.setDuration(0);
        showBackView.setDuration(1);
        rotateOut.setDuration(300);
        showBackView.setStartDelay(150);
        rotateOut.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addListener(new com.nineoldandroids.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(com.nineoldandroids.animation.Animator animation) {

            }

            @Override
            public void onAnimationEnd(com.nineoldandroids.animation.Animator animation) {
                cardSide = CARD_BACK;
            }

            @Override
            public void onAnimationCancel(com.nineoldandroids.animation.Animator animation) {

            }

            @Override
            public void onAnimationRepeat(com.nineoldandroids.animation.Animator animation) {

            }
        });
        set.play(flip).with(showBackView).before(rotateOut);
        set.start();
    }

    private void rotateOutToFrontBeforeEleven(){
        showFrontView();
        hideBackView();
        setBackgroundResource(R.drawable.cardbackground_sky);
        com.nineoldandroids.animation.AnimatorSet set = new com.nineoldandroids.animation.AnimatorSet();
        com.nineoldandroids.animation.ObjectAnimator flip = com.nineoldandroids.animation.ObjectAnimator.ofFloat(CreditCardView.this, "rotationY", 90, -90);
        com.nineoldandroids.animation.ObjectAnimator rotateOut = com.nineoldandroids.animation.ObjectAnimator.ofFloat(CreditCardView.this, "rotationY", -90, 0);
        com.nineoldandroids.animation.ObjectAnimator showFrontView = com.nineoldandroids.animation.ObjectAnimator.ofFloat(CreditCardView.this, "alpha", 0, 1);
        showFrontView.setDuration(1);
        rotateOut.setDuration(300);
        rotateOut.setInterpolator(new AccelerateDecelerateInterpolator());
        showFrontView.setStartDelay(150);
        set.addListener(new com.nineoldandroids.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(com.nineoldandroids.animation.Animator animation) {

            }

            @Override
            public void onAnimationEnd(com.nineoldandroids.animation.Animator animation) {
                cardSide = CARD_FRONT;
            }

            @Override
            public void onAnimationCancel(com.nineoldandroids.animation.Animator animation) {

            }

            @Override
            public void onAnimationRepeat(com.nineoldandroids.animation.Animator animation) {

            }
        });
        set.play(flip).with(showFrontView).with(rotateOut);
        set.start();
    }
}

