package pl.pola_app.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;
import com.journeyapps.barcodescanner.Size;
import com.journeyapps.barcodescanner.camera.CameraSettings;
import com.squareup.otto.Bus;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import pl.pola_app.PolaApplication;
import pl.pola_app.R;
import pl.pola_app.helpers.Utils;
import pl.pola_app.ui.activity.ActivityWebView;
import pl.pola_app.ui.event.FlashActionListener;
import pl.tajchert.nammu.Nammu;
import pl.tajchert.nammu.PermissionCallback;
import timber.log.Timber;

public class ScannerFragment extends Fragment implements CompoundBarcodeView.TorchListener, FlashActionListener {

    @Inject
    Bus eventBus;

    @BindView(R.id.scanner_view)
    CompoundBarcodeView barcodeScanner;//ZXING this or mPreview should be used

    @BindView(R.id.appIcon)
    ImageView appIcon;

    private boolean isTorchOn = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        PolaApplication.component(getActivity()).inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View scannerView = inflater.inflate(R.layout.fragment_scanner, container, false);
        ButterKnife.bind(this, scannerView);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        barcodeScanner.getBarcodeView().setFramingRectSize(new Size((int) (width * 0.9f), (int) (height * 0.25f)));
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        layoutParams.setMargins(0, (int) (-1 * (height * 0.2)), 0, 0);
        barcodeScanner.setLayoutParams(layoutParams);

        CameraSettings cameraSettings = barcodeScanner.getBarcodeView().getCameraSettings();
        //cameraSettings.setBarcodeSceneModeEnabled(true);
        cameraSettings.setContinuousFocusEnabled(true);
        cameraSettings.setAutoFocusEnabled(true);
        barcodeScanner.getBarcodeView().setCameraSettings(cameraSettings);

        barcodeScanner.setStatusText(getActivity().getString(R.string.scanner_status_text));
        barcodeScanner.setTorchListener(this);
        barcodeScanner.setTorchOff();
        Nammu.askForPermission(getActivity(), android.Manifest.permission.CAMERA, permissionCameraCallback);

        appIcon.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), ActivityWebView.class);
            intent.putExtra("url", Utils.URL_POLA_ABOUT);
            startActivity(intent);
        });

        return scannerView;
    }

    @Override
    public void onResume() {
        super.onResume();
        eventBus.register(this);
        if (barcodeScanner != null) {
            barcodeScanner.resume();
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        eventBus.unregister(this);
        if (barcodeScanner != null) {
            barcodeScanner.setTorchOff();
            barcodeScanner.pause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void resumeScanning() {
        if (barcodeScanner != null) {
            barcodeScanner.decodeContinuous(callback);
        }
    }

    final PermissionCallback permissionCameraCallback = new PermissionCallback() {
        @Override
        public void permissionGranted() {
            resumeScanning();
        }

        @Override
        public void permissionRefused() {
            Toast.makeText(getActivity(), getString(R.string.toast_no_camera_access), Toast.LENGTH_SHORT).show();
        }
    };

    //ZXING barcode result
    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(final BarcodeResult result) {
            if (result.getText() != null) {
                if (barcodeScanner != null) {
                    barcodeScanner.getBarcodeView().stopDecoding();
                    barcodeScanner.setStatusText("");
                }
                ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBarcode(result.getText());
                    }
                });

                Timber.d(result.getText());
                Timber.d(result.getBarcodeFormat().toString());

            }
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    private void onBarcode(String barcode) {
        final Activity activity = getActivity();
        if (activity != null && !activity.isFinishing() && activity instanceof BarcodeListener) {
            ((BarcodeListener) activity).onBarcode(barcode, true);
        }
    }

    @Override
    public boolean isTorchOn() {
        return isTorchOn;
    }

    @Override
    public void onFlashAction() {
        if (isTorchOn) {
            barcodeScanner.setTorchOff();
        } else {
            barcodeScanner.setTorchOn();
        }
    }

    @Override
    public void onTorchOn() {
        isTorchOn = true;
    }

    @Override
    public void onTorchOff() {
        isTorchOn = false;
    }

    public void setTorchOff() {
        if (isTorchOn) {
            barcodeScanner.setTorchOff();
        }
    }
}
