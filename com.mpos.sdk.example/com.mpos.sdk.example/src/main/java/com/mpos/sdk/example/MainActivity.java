package com.mpos.sdk.example;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import androidx.fragment.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.mpos.sdk.PaymentController;
import com.mpos.sdk.PaymentController.ReaderType;
import com.mpos.sdk.PaymentControllerException;
import com.mpos.sdk.entities.APIAuthResult;
import com.mpos.sdk.entities.Account;
import com.mpos.sdk.entities.LinkedCard;
import com.mpos.sdk.entities.PaymentProductItem;
import com.mpos.sdk.example.fragments.FragmentCards;
import com.mpos.sdk.example.fragments.FragmentHistory;
import com.mpos.sdk.example.fragments.FragmentStuff;
import com.mpos.sdk.example.fragments.FragmentPayment;
import com.mpos.sdk.example.fragments.FragmentPrinter;
import com.mpos.sdk.example.fragments.FragmentScanner;
import com.mpos.sdk.example.fragments.FragmentSettings;

public class MainActivity extends PermissionsActivity implements TabHost.OnTabChangeListener {

	public Account Account;
	public String BankName;
	public String ClientName;
	public String ClientLegalName;
	public String ClientPhone;
	public String ClientWeb;
	public HashMap<PaymentController.PaymentMethod, Map<String, String>> AcquirersByMethods;
	public List<LinkedCard> LinkedCards;
	public ArrayList<PaymentProductItem> Products;

	public static final PaymentController.Currency CURRENCY = PaymentController.Currency.EUR;

	private FragmentTabHost mTabHost;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		setContentView(R.layout.activity_main);
		((TextView) findViewById(R.id.version)).setText(getString(R.string.app_name) + " " + PaymentController.VERSIONCODE
		+ "\n" + Build.MANUFACTURER + " " + Build.MODEL + " " + Build.VERSION.RELEASE);

		mTabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
		mTabHost.setOnTabChangedListener(this);

		PaymentController.CHECK_CONFIG_BEFORE_PAYMENT = false;
		PaymentController.getInstance().onCreate(this, savedInstanceState);
		PaymentController.getInstance().setHashSalt("1234");
		PaymentController.getInstance().setHashType(PaymentController.HashType.SHA256);
		String readerType = Utils.getString(this, Consts.SavedParams.READER_TYPE_KEY);
		String readerAddress = Utils.getString(this, Consts.SavedParams.READER_ADDRESS_KEY);
		if (!PaymentController.getInstance().isPaymentInProgress()) {
			if (readerType != null && readerType.length() > 0) {
				try {
					PaymentController.getInstance().setReaderType(this, ReaderType.valueOf(readerType), readerAddress);
					if (ReaderType.valueOf(readerType) == ReaderType.SOFTPOS) {
						String accessCode = Utils.getString(this, Consts.SavedParams.READER_ACCESS_CODE);
						Hashtable<String, Object> p = new Hashtable<>();
						p.put("AccessCode", accessCode);
						p.put("PackageName", Utils.getString(this, Consts.SavedParams.READER_SOFTPOS_PACKAGE));
						PaymentController.getInstance().setCustomReaderParams(p);
					} else if (ReaderType.valueOf(readerType) == ReaderType.P17) {
						Hashtable<String, Object> p = new Hashtable<>();
						p.put("NOTUP", true);
						PaymentController.getInstance().setCustomReaderParams(p);
					}
				} catch (PaymentControllerException e) {
					Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}

		//PaymentController.setRequestType(PaymentController.RequestType.URLENCODED);

		LinkedCards = new ArrayList<>();
		if (savedInstanceState != null) {
			Account = (Account) savedInstanceState.getSerializable(getClass().getCanonicalName() + ".Account");
			if (Account != null) {
				BankName = Account.getBankName();
				ClientName = Account.getClientName();
				ClientLegalName = Account.getClientLegalName();
				ClientPhone = Account.getClientPhone();
				ClientWeb = Account.getClientWeb();
				AcquirersByMethods = Account.getAcquirersByMethods();
				if (Account.getLinkedCards() != null)
					LinkedCards.addAll(Account.getLinkedCards());
			}
			Products = (ArrayList<PaymentProductItem>) savedInstanceState.getSerializable(getClass().getCanonicalName() + ".Products");
		}

		else if (PaymentController.getInstance().getReaderType() == null)
			try {
				PaymentController.getInstance().setReaderType(this, ReaderType.P17, null);
			} catch (PaymentControllerException e) {
				e.printStackTrace();
			}

	 	PaymentController.getInstance().setSingleStepEMV(true);
		PaymentController.getInstance().setRepeatOnError(false);
		PaymentController.getInstance().setClientProductCode(getString(R.string.app_name));
		if (Account == null)
			showLoginDialog();
		else
			setupTabHost();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		PaymentController.getInstance().onSaveInstanceState(outState);

		outState.putSerializable(getClass().getCanonicalName() + ".Account", Account);
		outState.putSerializable(getClass().getCanonicalName() + ".Products", Products);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onDestroy() {
		PaymentController.getInstance().onDestroy();
		super.onDestroy();
	}

	@Override
	public void onTabChanged(String s) {
		if (s.equalsIgnoreCase(getString(R.string.tab_payment)) && !PaymentController.getInstance().isPaymentInProgress()) {
			try {
				PaymentController.getInstance().initPaymentSession();
			} catch (PaymentControllerException e) {
				Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}

	private void setupTabHost() {
		mTabHost.setup(this, getSupportFragmentManager(),android.R.id.tabcontent);
		
		mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.tab_payment)).setIndicator(getString(R.string.tab_payment)), 
				FragmentPayment.class, null);
		mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.tab_history)).setIndicator(getString(R.string.tab_history)), 
				FragmentHistory.class, null);
		mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.tab_cards)).setIndicator(getString(R.string.tab_cards)),
				FragmentCards.class, null);
		mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.tab_print)).setIndicator(getString(R.string.tab_print)),
				FragmentPrinter.class, null);
		mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.tab_scan)).setIndicator(getString(R.string.tab_scan)),
				FragmentScanner.class, null);
		mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.tab_mifare)).setIndicator(getString(R.string.tab_mifare)),
				FragmentStuff.class, null);
		mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.tab_settings)).setIndicator(getString(R.string.tab_settings)),
				FragmentSettings.class, null);
	}

	private void showLoginDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.login_dlg_title))
			.setCancelable(false)
			.setPositiveButton(getString(R.string.login_dlg_btn_ok), new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(final DialogInterface dialog, int which) {
					String login = ((EditText)((AlertDialog)dialog).findViewById(R.id.login_dlg_edt_login)).getText().toString();
					String password = ((EditText)((AlertDialog)dialog).findViewById(R.id.login_dlg_edt_password)).getText().toString();
					try {
						PaymentController.getInstance().setCredentials(login, password);
						APIAuthResult result = PaymentController.getInstance().auth(MainActivity.this);
						LinkedCards = new ArrayList<LinkedCard>();
						if (result == null) {
							Toast.makeText(MainActivity.this, "Connection lost", Toast.LENGTH_LONG).show();
							mTabHost.postDelayed(new Runnable() {
								@Override
								public void run() {
									((AlertDialog) dialog).show();
								}
							}, 300);
						} else if (!result.isValid()) {
							Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_LONG).show();
							mTabHost.postDelayed(new Runnable() {
								@Override
								public void run() {
									((AlertDialog) dialog).show();
								}
							}, 300);
						} else {
							Account = result.getAccount();
							BankName = result.getAccount().getBankName();
							ClientName = result.getAccount().getClientName();
							ClientLegalName = result.getAccount().getClientLegalName();
							ClientPhone = result.getAccount().getClientPhone();
							ClientWeb = result.getAccount().getClientWeb();
							AcquirersByMethods = result.getAccount().getAcquirersByMethods();
							if (result.getAccount().getLinkedCards() != null)
								LinkedCards.addAll(result.getAccount().getLinkedCards());
							Products = result.getProducts();
							dialog.dismiss();
							setupTabHost();
						}
					} catch (PaymentControllerException e) {
						Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
					}
				}
			})
			.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			})
			.setView(LayoutInflater.from(this).inflate(R.layout.dialog_login, null))
			.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();
				}
			})
			.create()
			.show();
	}
}
