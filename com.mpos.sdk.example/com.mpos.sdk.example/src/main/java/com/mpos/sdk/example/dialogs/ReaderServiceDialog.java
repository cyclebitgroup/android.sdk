package com.mpos.sdk.example.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.mpos.sdk.PaymentController;
import com.mpos.sdk.PaymentControllerException;
import com.mpos.sdk.PaymentControllerListener;
import com.mpos.sdk.PaymentResultContext;
import com.mpos.sdk.example.R;

public abstract class ReaderServiceDialog extends Dialog implements PaymentControllerListener {
    private final boolean interceptController;

    private TextView lblState;
    private ImageView imgSpinner;
    private RotateAnimation spinRotation;

    public ReaderServiceDialog(Context context, boolean interceptController) {
        super(context);
        this.interceptController = interceptController;
        init();
    }

    public ReaderServiceDialog(Context context) {
        this(context, true);
    }

    private void init() {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_payment);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;

        lblState = (TextView) findViewById(R.id.payment_dlg_lbl_state);
        imgSpinner = (ImageView) findViewById(R.id.payment_dlg_spinner);

        spinRotation = new RotateAnimation(
                0f,
                360f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        spinRotation.setDuration(1200);
        spinRotation.setInterpolator(new LinearInterpolator());
        spinRotation.setRepeatMode(Animation.RESTART);
        spinRotation.setRepeatCount(Animation.INFINITE);

        if (interceptController) {
            PaymentController.getInstance().setPaymentControllerListener(this);
            PaymentController.getInstance().enable();
        }
    }

    public void setText(String text) {
        lblState.setText(text);
    }

    public void setText(int stringId) {
        lblState.setText(getContext().getString(stringId));
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (interceptController) {
            PaymentController.getInstance().setPaymentControllerListener(null);
            PaymentController.getInstance().disable();
        }
    }

    @Override
    public void dismiss() {
        stopProgress();
        super.dismiss();
    }

    protected abstract void startServiceAction() throws PaymentControllerException;

    protected void startProgress() {
        imgSpinner.setVisibility(View.VISIBLE);
        imgSpinner.startAnimation(spinRotation);
    }

    protected void setProgressText(String text) {
        lblState.setText(text);
    }

    protected void stopProgress() {
        imgSpinner.clearAnimation();
        imgSpinner.setVisibility(View.GONE);
    }

    @Override
    public void onReaderEvent(PaymentController.ReaderEvent event, Map<String, String> params) {
        switch (event) {
            case CONNECTED:
            case START_INIT:
                lblState.setText(R.string.reader_state_init);
                break;
            case DISCONNECTED:
                stopProgress();
                lblState.setText(R.string.reader_state_disconnected);
                break;
            case INIT_SUCCESSFULLY:
                Log.i("mposSDK", "readerInfo: " + params);
                lblState.setText(R.string.progress);
                startProgress();
                try {
                    startServiceAction();
                } catch (PaymentControllerException e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    dismiss();
                }
                break;
            case INIT_FAILED:
                stopProgress();
                lblState.setText(R.string.reader_state_init_error);
                break;
            default:
                break;
        }
    }


    @Override
    public void onAutoConfigFinished(boolean success, String config, boolean isDefault) {

    }

    @Override
    public void onTransactionStarted(String transactionID) {

    }

    @Override
    public void onFinished(PaymentResultContext result) {

    }

    @Override
    public void onError(PaymentController.PaymentError error, String errorMessage, int extErrorCode) {

    }

    @Override
    public int onSelectApplication(List<String> apps) {
        return 0;
    }

    @Override
    public boolean onConfirmSchedule(List<Map.Entry<Date, Double>> steps, double totalAmount) {
        return false;
    }

    @Override
    public boolean onScheduleCreationFailed(PaymentController.PaymentError error, String errorMessage, int extErrorCode) {
        return false;
    }

    @Override
    public boolean onCancellationTimeout() {
        return false;
    }

    @Override
    public void onPinRequest() {

    }

    @Override
    public void onPinEntered() {

    }

    @Override
    public void onBatteryState(double percent) {
        Toast.makeText(getContext(),
                String.format(getContext().getString(R.string.payment_toast_battery_format), percent),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public PaymentController.PaymentInputType onSelectInputType(List<PaymentController.PaymentInputType> allowedInputTypes) {
        return null;
    }

    @Override
    public void onSwitchedToCNP() {

    }

    @Override
    public void onInjectFinished(boolean success) {

    }

    @Override
    public void onBarcodeScanned(String barcode) {

    }

    @Override
    public boolean onBLCheck(String hashPan, String last4digits) {
        return false;
    }

    @Override
    public void onReaderConfigFinished(boolean success) {

    }

    @Override
    public void onReaderConfigUpdate(String config, Hashtable<String, Object> params) {

    }
}
