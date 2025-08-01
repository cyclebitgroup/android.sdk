package com.mpos.sdk.example.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import com.mpos.sdk.PaymentController;
import com.mpos.sdk.PaymentResultContext;
import com.mpos.sdk.entities.APITryGetPaymentStatusResult;
import com.mpos.sdk.entities.Purchase;
import com.mpos.sdk.entities.ScheduleItem;
import com.mpos.sdk.entities.TransactionItem;
import com.mpos.sdk.example.CommonAsyncTask;
import com.mpos.sdk.example.MainActivity;
import com.mpos.sdk.example.R;
import com.mpos.sdk.example.Utils;

public class ResultDialog extends Dialog {
    private TextView lblOperation, lblState, lblID, lblInvoice, lblExtID, lblAppcode, lblTerminal,
            lblIIN, lblPAN, lblPANFull, lblTrack2, lblLink,
            lblEMV, lblSignature, lblAuxData, lblExtTranData;
    private Button btnAdjust;

    private PaymentResultContext mPaymentResultContext;
    public ResultDialog(final Context context, final PaymentResultContext paymentResultContext, final boolean isReverse) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;
        setContentView(R.layout.dialog_tr_details);

        initControls();
        mPaymentResultContext = paymentResultContext;
        final TransactionItem transactionItem = paymentResultContext.getTransactionItem();
        final ScheduleItem scheduleItem = paymentResultContext.getScheduleItem();
        final boolean isRegular = scheduleItem != null;

        if (isRegular)
            update(scheduleItem);
        else
            update(transactionItem);
        lblTerminal.setText(paymentResultContext.getTerminalName());
        lblSignature.setText(String.valueOf(paymentResultContext.isRequiresSignature()));
        if (!isRegular && paymentResultContext.getEmvData() != null) {
            StringBuilder emvData = new StringBuilder();
            Iterator<Map.Entry<String, String>> iterator = paymentResultContext.getEmvData().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> nextPair = iterator.next();
                emvData.append(nextPair.getKey()).append(" : ").append(nextPair.getValue());
                if (iterator.hasNext())
                    emvData.append("\n");
            }
            lblEMV.setText(emvData.toString());
        }

        btnAdjust.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismiss();
                new AdjustDialog(getContext(), isRegular ? String.valueOf(scheduleItem.getID()) : transactionItem.getID(), isRegular, isReverse).show();
            }
        });


        if (transactionItem == null) {
            LinearLayout container = (LinearLayout) lblState.getParent();

            container.getChildAt(container.indexOfChild(lblState) - 1).setVisibility(View.GONE);
            lblState.setVisibility(View.GONE);
            container.getChildAt(container.indexOfChild(lblInvoice) - 1).setVisibility(View.GONE);
            lblInvoice.setVisibility(View.GONE);
            container.getChildAt(container.indexOfChild(lblExtID) - 1).setVisibility(View.GONE);
            lblExtID.setVisibility(View.GONE);
            container.getChildAt(container.indexOfChild(lblAppcode) - 1).setVisibility(View.GONE);
            lblAppcode.setVisibility(View.GONE);
            container.getChildAt(container.indexOfChild(lblTerminal) - 1).setVisibility(View.GONE);
            lblTerminal.setVisibility(View.GONE);
            container.getChildAt(container.indexOfChild(lblLink) - 1).setVisibility(View.GONE);
            lblLink.setVisibility(View.GONE);
            container.getChildAt(container.indexOfChild(lblEMV) - 1).setVisibility(View.GONE);
            lblEMV.setVisibility(View.GONE);
        }
    }

    private void initControls() {
        lblOperation	= (TextView)findViewById(R.id.tr_details_dlg_lbl_operation);
        lblState        = (TextView)findViewById(R.id.tr_details_dlg_lbl_state);
        lblID 			= (TextView)findViewById(R.id.tr_details_dlg_lbl_id);
        lblInvoice 		= (TextView)findViewById(R.id.tr_details_dlg_lbl_invoice);
        lblExtID 		= (TextView)findViewById(R.id.tr_details_dlg_lbl_extid);
        lblExtTranData  = (TextView)findViewById(R.id.tr_details_dlg_lbl_exttrandata);
        lblAppcode 		= (TextView)findViewById(R.id.tr_details_dlg_lbl_appcode);
        lblTerminal 	= (TextView)findViewById(R.id.tr_details_dlg_lbl_terminal);
        lblIIN  		= (TextView)findViewById(R.id.tr_details_dlg_lbl_iin);
        lblPAN 			= (TextView)findViewById(R.id.tr_details_dlg_lbl_pan);
        lblPANFull  	= (TextView)findViewById(R.id.tr_details_dlg_lbl_pan_full);
        lblTrack2		= (TextView)findViewById(R.id.tr_details_dlg_lbl_track2);
        lblEMV          = (TextView)findViewById(R.id.tr_details_dlg_lbl_emv);
        lblLink         = (TextView)findViewById(R.id.tr_details_dlg_lbl_link);
        lblSignature 	= (TextView)findViewById(R.id.tr_details_dlg_lbl_signature);
        lblAuxData      = (TextView)findViewById(R.id.tr_details_dlg_lbl_auxdata);
        btnAdjust 		= (Button)findViewById(R.id.tr_details_dlg_btn_adjust);
    }

    private void update(TransactionItem transactionItem) {
        lblOperation.setText(transactionItem.getOperation());
        lblState.setText(new StringBuilder(transactionItem.getStateDisplay()).append(" (").append(transactionItem.getSubStateDisplay()).append(")"));
        lblID.setText(transactionItem.getID());
        lblInvoice.setText(transactionItem.getInvoice());
        lblExtID.setText(transactionItem.getExtID());
        lblExtTranData.setText(transactionItem.getExtTranData());
        lblAppcode.setText(transactionItem.getApprovalCode());

        lblIIN.setText(transactionItem.getCard().getIin());
        lblPAN.setText(transactionItem.getCard().getPanMasked().replace("*", " **** ") + "\nHash: " + mPaymentResultContext.getCardHash());
        lblPANFull.setText(transactionItem.getCard().getPANFull());
        lblTrack2.setText(transactionItem.getCard().getTrack2());

        TransactionItem.ExternalPayment externalPayment = transactionItem.getExternalPayment();
        if (externalPayment != null) {
            if (externalPayment.getType() == TransactionItem.ExternalPayment.Type.QR) {
                lblLink.setText("QR: " + Arrays.toString(externalPayment.getQR().toArray()));
            } else if (externalPayment.getType() == TransactionItem.ExternalPayment.Type.LINK) {
                lblLink.setText(externalPayment.getLink());
            }
        }
    }

    private void update(ScheduleItem scheduleItem) {
        lblOperation.setText("SCHEDULE");
        lblState.setText("");
        lblID.setText(String.valueOf(scheduleItem.getID()));
        lblInvoice.setText("");
        lblExtID.setText("");
        lblAppcode.setText("");

        lblIIN.setText(scheduleItem.getCard().getIin());
        lblPAN.setText(scheduleItem.getCard().getPanMasked().replace("*", " **** "));
        lblPANFull.setText(scheduleItem.getCard().getPANFull());
        lblTrack2.setText(scheduleItem.getCard().getTrack2());
    }
}
