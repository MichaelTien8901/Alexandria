package it.jaschke.alexandria;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class BookDetailActivity extends ActionBarActivity implements BookDetail.Callbacks {
    @Override
    public void OnBackPressed() {
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ( getResources().getBoolean(R.bool.has_two_panes)) {
            finish();
            return;
        }
        setContentView(R.layout.activity_book_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (savedInstanceState == null) {
            // create fragment passing argument
            String ean = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            Bundle args = new Bundle();
            args.putString(BookDetail.EAN_KEY, ean);
            args.putBoolean(BookDetail.BACK_KEY, true);
            BookDetail fragment = new BookDetail();
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.book_detail_container, fragment)
                    .commit();
        }
    }

}
