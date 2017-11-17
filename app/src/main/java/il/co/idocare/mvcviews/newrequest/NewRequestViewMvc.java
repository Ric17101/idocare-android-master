package il.co.idocare.mvcviews.newrequest;

import il.co.idocare.Constants;
import il.co.idocare.mvcviews.ObservableViewMvc;

/**
 * MVC View for New Request screen
 */
public interface NewRequestViewMvc
        extends ObservableViewMvc<NewRequestViewMvc.NewRequestViewMvcListener> {


    interface NewRequestViewMvcListener {
        /**
         * Will be invoked when the user clicks on "create request" button
         */
        void onCreateRequestClicked();

        /**
         * Will be invoked when the user clicks on "take picture" button
         */
        void onTakePictureClicked();
    }

    String KEY_CREATED_COMMENT = Constants.FIELD_NAME_CREATED_COMMENT;


    int MAX_PICTURES = 3;

    /**
     * Show picture at position
     * @param position the position (i.e. index) of the picture
     * @param cameraPicturePath picture's URI
     */
    void showPicture(int position, String cameraPicturePath);
}
