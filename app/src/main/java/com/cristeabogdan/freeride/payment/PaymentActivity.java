package com.cristeabogdan.freeride.payment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cristeabogdan.freeride.databinding.ActivityPaymentBinding;
import com.cristeabogdan.simulator.Simulator;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.DirectionsApi;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.cristeabogdan.freeride.BuildConfig;

import java.text.DecimalFormat;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONObject;

public class PaymentActivity extends AppCompatActivity {
    private static final String TAG = "PaymentActivity";
    private static final String STRIPE_SECRET_KEY = BuildConfig.STRIPE_SECRET_KEY;
    private static final double PER_KM      = 0.30;
    private static final double PER_MIN     = 1.00;
    private static final double SERVICE_FEE = 1.09;
    public static double totalFare, KM, MINUTES;


    private ActivityPaymentBinding binding;
    private String paymentLinkUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.payButton.setEnabled(false);
        binding.payButton.setOnClickListener(v -> {
            if (paymentLinkUrl != null) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(paymentLinkUrl)));
            } else {
                Toast.makeText(this, "Link not ready", Toast.LENGTH_SHORT).show();
            }
        });

        SharedPreferences prefs = getSharedPreferences("RidePrefs", MODE_PRIVATE);
        LatLng pickUp = new LatLng(
                prefs.getFloat("pickup_lat", 0f),
                prefs.getFloat("pickup_lng", 0f)
        );
        LatLng drop = new LatLng(
                prefs.getFloat("drop_lat", 0f),
                prefs.getFloat("drop_lng", 0f)
        );

        fetchComputeAndBuildLink(pickUp, drop);
    }

    private void fetchComputeAndBuildLink(LatLng origin, LatLng dest) {
        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            try {
                // 1) route info
                DirectionsResult res = DirectionsApi.newRequest(Simulator.geoApiContext)
                        .origin(origin.latitude + "," + origin.longitude)
                        .destination(dest.latitude  + "," + dest.longitude)
                        .await();
                DirectionsLeg leg = res.routes[0].legs[0];
                double km   = leg.distance.inMeters / 1000.0;
                double mins = leg.duration.inSeconds / 60.0;
                DecimalFormat kmFmt  = new DecimalFormat("0.###");   // up to 3 decimals, adjust as needed
                DecimalFormat minFmt = new DecimalFormat("0");       // integer minute
                KM = Double.parseDouble(kmFmt.format(km));
                MINUTES = Double.parseDouble(minFmt.format(Math.min((long)mins, 9999)));  // cap at 4 digits if needed;

                // breakdown
                double baseFare     = mins * PER_MIN;
                double distanceFare = km   * PER_KM;
                double serviceFee   = SERVICE_FEE;
                double total        = Math.round((baseFare + distanceFare + serviceFee) * 100) / 100.0;
                totalFare = total;
                int amountCents     = (int)(total * 100);

                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    DecimalFormat df = new DecimalFormat("#.##");
                    binding.tripDistanceTextView.setText(df.format(km) + " km");
                    binding.tripDurationTextView.setText((int)Math.ceil(mins) + " min");
                    binding.tripAmountTextView.setText("$" + df.format(total));
                    binding.baseFareAmountTextView    .setText("$" + df.format(baseFare));
                    binding.distanceFareAmountTextView.setText("$" + df.format(distanceFare));
                    binding.serviceFeeAmountTextView   .setText("$" + df.format(serviceFee));
                    binding.totalAmountTextView        .setText("$" + df.format(total));
                });

                // 2) create Stripe Price
                FormBody priceBody = new FormBody.Builder()
                        .add("unit_amount", String.valueOf(amountCents))
                        .add("currency",    "usd")
                        .add("product_data[name]", "Ride Fare")
                        .build();
                Request priceReq = new Request.Builder()
                        .url("https://api.stripe.com/v1/prices")
                        .post(priceBody)
                        .header("Authorization", "Bearer " + STRIPE_SECRET_KEY)
                        .build();
                JSONObject priceJson = new JSONObject(client.newCall(priceReq).execute().body().string());
                String priceId = priceJson.getString("id");

                // 3) create Payment Link
                FormBody linkBody = new FormBody.Builder()
                        .add("line_items[0][price]", priceId)
                        .add("line_items[0][quantity]", "1")
                        .add("after_completion[type]", "redirect")
                        .add("after_completion[redirect][url]", "freeride://payment/success")
                        .build();
                Request linkReq = new Request.Builder()
                        .url("https://api.stripe.com/v1/payment_links")
                        .post(linkBody)
                        .header("Authorization", "Bearer " + STRIPE_SECRET_KEY)
                        .build();
                Response linkResp = client.newCall(linkReq).execute();
                String linkRespBody = linkResp.body().string();
                Log.d(TAG, "Link response: " + linkRespBody);
                JSONObject linkJson = new JSONObject(linkRespBody);
                paymentLinkUrl = linkJson.getString("url");

                runOnUiThread(() -> binding.payButton.setEnabled(true));
            } catch (Exception e) {
                Log.e(TAG, "could not build link", e);
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Error generating link", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
