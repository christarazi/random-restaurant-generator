package com.chris.randomrestaurantgenerator;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.chris.randomrestaurantgenerator.fragments.MainActivityFragment;
import com.chris.randomrestaurantgenerator.utils.LocationProviderHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_saved_list) {
            Intent intent = new Intent(this, SavedListActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    // Function to be called from MainActivityFragment to return the menu button view for the savedList.
    public View getMenuItemView() {
        return findViewById(R.id.action_saved_list);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LocationProviderHelper.MY_LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    MainActivityFragment fragment = (MainActivityFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.mainFragment);
                    fragment.reactToPermissionsCallback(true);
                } else {


                    MainActivityFragment fragment = (MainActivityFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.mainFragment);
                    fragment.reactToPermissionsCallback(false);
                }
            }
        }
    }
}
