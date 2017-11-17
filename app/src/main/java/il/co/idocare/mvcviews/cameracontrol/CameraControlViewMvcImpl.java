package il.co.idocare.mvcviews.cameracontrol;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;

import il.co.idocare.Constants;
import il.co.idocare.R;
import il.co.idocare.mvcviews.AbstractViewMvc;

/**
 * Implementation of CameraControlViewMvc interface
 */
public class CameraControlViewMvcImpl
        extends AbstractViewMvc<CameraControlViewMvc.CameraControlViewMvcListener>
        implements CameraControlViewMvc {

    private static final String TAG = "CameraControlViewMvc";

    private ImageView[] mImgPictures = new ImageView[CameraControlViewMvc.MAX_PICTURES];

    public CameraControlViewMvcImpl(LayoutInflater inflater, ViewGroup container) {
        setRootView(inflater.inflate(R.layout.element_camera_control, container, false));

        mImgPictures[0] = (ImageView) getRootView().findViewById(R.id.img_picture0);
        mImgPictures[1] = (ImageView) getRootView().findViewById(R.id.img_picture1);
        mImgPictures[2] = (ImageView) getRootView().findViewById(R.id.img_picture2);


        View viewTakePicture = getRootView().findViewById(R.id.view_take_picture);
        viewTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (CameraControlViewMvcListener listener : getListeners()) {
                    listener.onTakePictureClicked();
                }
            }
        });
    }

    @Override
    public void showPicture(int position, String cameraPicturePath) {
        if (position >= CameraControlViewMvc.MAX_PICTURES) {
            throw new IllegalArgumentException("maximal number of pictures exceeded!");
        }

        ImageLoader.getInstance().displayImage(
                Constants.UIL_LOCAL_FILE_PREFIX + cameraPicturePath,
                mImgPictures[position],
                Constants.DEFAULT_DISPLAY_IMAGE_OPTIONS);
    }

    @Override
    public Bundle getViewState() {
        return null;
    }
}
