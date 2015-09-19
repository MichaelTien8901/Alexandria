package it.jaschke.alexandria;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import net.sourceforge.zbar.Symbol;

import it.jaschke.alexandria.camera.ZBarConstants;
import it.jaschke.alexandria.camera.ZBarScannerActivity;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;

public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private final String LOG_TAG = AddBook.class.getSimpleName();

    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    private View rootView;
    private TextView ean;

    private final int LOADER_ID = 1;
    private final String EAN_CONTENT="eanContent";
    private static final String SCAN_FORMAT = "scanFormat";
    private static final String SCAN_CONTENTS = "scanContents";
    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";
    static final int ZXING_INTERNAL_REQUEST_CODE = 100;
    static final int ZBAR_SCANNER_REQUEST = 101;
    static final int ZXING_EXTERNAL_REQUEST_CODE = 102;
    public static final String BROADCAST_ACTION =
            "com.example.alexandria.BROADCAST";


    public AddBook(){}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
//        getLoaderManager().initLoader(LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ZXING_EXTERNAL_REQUEST_CODE:
            if (resultCode == Activity.RESULT_OK) {
                ean.setText(data.getStringExtra("SCAN_RESULT"));
            }
            break;
            case ZXING_INTERNAL_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    ean.setText(data.getStringExtra(ZxingScannerActivity.SCAN_RESULT));
                }
                break;
            case ZBAR_SCANNER_REQUEST:
            if (resultCode == Activity.RESULT_OK) {
                ean.setText(data.getStringExtra("SCAN_RESULT"));
            }
            break;
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ean = (EditText) rootView.findViewById(R.id.ean);
        if(savedInstanceState!=null){
//            ean.setText(savedInstanceState.getString(EAN_CONTENT));
//            ean.setHint("");
        }

        ean.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                // prevent reload after configuration change
                String ean = s.toString();
                //catch isbn10 numbers
                if (ean.length() == 10 && !ean.startsWith("978")) {
                    ean = "978" + ean;
                }
                if (ean.length() < 13) {
                    return;
                }
//                clearFields();
                //Once we have an ISBN, start a book intent
                clearBookStatus(getActivity());
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean);
                bookIntent.setAction(BookService.FETCH_BOOK);
                getActivity().startService(bookIntent);
                AddBook.this.restartLoader();
            }
        });

        rootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get preference to find out which scanner to use
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String scanner = sharedPref.getString(getString(R.string.pref_camera_key), getString(R.string.pref_camera_default));
                if ( scanner.equals(getString(R.string.zbar_value))) {
                    // use internal ZBar scanner
                    Intent intent = new Intent(getActivity(), ZBarScannerActivity.class);
                    intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{Symbol.EAN13});
                    startActivityForResult(intent, ZBAR_SCANNER_REQUEST);
                } else if ( scanner.equals(getString(R.string.zxing_internal_value))) {
                    Intent intent = new Intent(getActivity(), ZxingScannerActivity.class);
//                    intent.putExtra(ZxingScannerActivity.FLASH_STATE, false);
//                    intent.putExtra(ZxingScannerActivity.SELECTED_FORMATS, new String[] {"EAN13"});
                    startActivityForResult(intent, ZXING_INTERNAL_REQUEST_CODE);    //Barcode Scanner to scan for us
                } else {
                    // use external ZXing scanner
                    Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                    intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
                    intent.putExtra("SCAN_FORMATS", "EAN13,EAN8");
                    try {
                        startActivityForResult(intent, ZXING_EXTERNAL_REQUEST_CODE);    //Barcode Scanner to scan for us
                    } catch (Exception e) {
                        // dialog to ask installation
                        AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                        alertDialog.setTitle("");
                        alertDialog.setMessage("Barcode Scanner not found.  Do you want to install it?");

                        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.zxing.client.android")));
                            }
                        });
                        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // do nothing
                            }
                        });
                        alertDialog.show();
                    }
                }
            }
        });

        rootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ean.setText("");
                clearFields();
            }
        });

        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean.getText().toString());
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                ean.setText("");
                clearFields();
            }
        });
        return rootView;
    }
    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String eanStr= ean.getText().toString();

        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        updateEmptyView();
        if (!data.moveToFirst()) {
            return;
        }
        // check rootView in case configuration changed
        if ( rootView == null) {
            Log.d(LOG_TAG, "OnLoadFinished: rootView not found");
            return;
        }
        clearFields();
        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        // check before use it
        if ( authors != null && authors.length() != 0 ) {
            String[] authorsArr = authors.split(",");
            ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
            ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",","\n"));
        } else {
            Log.v(LOG_TAG, "author is empty");
        }
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if(Patterns.WEB_URL.matcher(imgUrl).matches()){
            //new DownloadImage((ImageView) rootView.findViewById(R.id.bookCover)).execute(imgUrl);
            Picasso.with(getActivity())
                    .load(imgUrl)
                    .placeholder(R.drawable.ic_launcher)
                    .error(R.drawable.ic_launcher)
                    .into((ImageView) rootView.findViewById(R.id.bookCover));
            rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
        } else {
            ((ImageView) rootView.findViewById(R.id.fullBookCover)).setImageResource(R.drawable.ic_launcher);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        ((TextView) rootView.findViewById(R.id.categories)).setText(categories);

        rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields(){
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.authors)).setText("");
        ((TextView) rootView.findViewById(R.id.categories)).setText("");
//        ((TextView) rootView.findViewById(R.id.empty_view)).setText("");
        rootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }
    public void updateEmptyView() {
        @BookService.GoogleBookStatus int status = getGoogleBookStatus(getContext());
        TextView tv = (TextView) getView().findViewById(R.id.empty_view);
        View container = getView().findViewById(R.id.add_book_container);
        if ( null != tv  && null != container) {
            // if cursor is empty, why? do we have an invalid location
            int message;
            switch (status) {
                case BookService.BOOK_STATUS_NO_NETWORK:
                    message = R.string.empty_book_no_network;
                    break;
                case BookService.BOOK_STATUS_SERVER_DOWN:
                    message = R.string.empty_book_server_down;
                    break;
                case BookService.BOOK_STATUS_SERVER_INVALID:
                    message = R.string.empty_book_server_error;
                    break;
                case BookService.BOOK_STATUS_NO_BOOK_FOUND:
                    message = R.string.empty_book_no_book;
                    // use toast
                    Toast toast = Toast.makeText(getActivity(), message, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);;
                    toast.show();;
                    return;
//                    break;
                default:
                    if (!isNetworkAvailable(getContext()) ) {
                        message = R.string.empty_book_no_network;
                        break;
                    } else {
                        tv.setVisibility(View.INVISIBLE);
                        container.setVisibility(View.VISIBLE);
                        return;
                    }
            }
            tv.setText(message);
            tv.setVisibility(View.VISIBLE);
            container.setVisibility(View.INVISIBLE);
        }
    }
    @SuppressWarnings("ResourceType")
    static public @BookService.GoogleBookStatus
    int getGoogleBookStatus(Context c){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getInt(c.getString(R.string.pref_book_status_key), BookService.BOOK_STATUS_UNKNOWN);
    }

    static private void clearBookStatus(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor spe = sp.edit();
        spe.putInt(c.getString(R.string.pref_book_status_key), BookService.BOOK_STATUS_UNKNOWN);
        spe.commit();
    }
    /**
     * Returns true if the network is available or about to become available.
     *
     * @param c Context used to get the ConnectivityManager
     * @return true if the network is available
     */
    static public boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

}
