package com.example.android.sqliteweather;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.sqliteweather.data.ForecastItem;
import com.example.android.sqliteweather.data.ForecastLocation;
import com.example.android.sqliteweather.data.Status;
import com.example.android.sqliteweather.utils.OpenWeatherMapUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements ForecastAdapter.OnForecastItemClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        LocationsAdapter.OnLocationItemClickListener,
        BottomNavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnNavigationItemReselectedListener{


    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView mForecastLocationTV;
    private RecyclerView mForecastItemsRV;
    private RecyclerView mLocationItemsRV;
    private ProgressBar mLoadingIndicatorPB;
    private TextView mLoadingErrorMessageTV;
    private DrawerLayout mDrawerLayout;
    private BottomNavigationView mBottomNavBar;
    private LocationsAdapter mLocationsAdapter;
    private ForecastAdapter mForecastAdapter;
    private ForecastViewModel mForecastViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        // NavBar!
        mBottomNavBar = findViewById(R.id.bottom_navigationbar);
        mBottomNavBar.setOnNavigationItemSelectedListener(this);

        // End NavBar
        NavigationView navigationView = findViewById(R.id.nv_nav_drawer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);


        /* SQLITE CHANGES */
        mLocationItemsRV = navigationView.findViewById(R.id.rv_location_items);

        mLocationsAdapter = new LocationsAdapter(this);
        mLocationItemsRV.setAdapter(mLocationsAdapter);
        mLocationItemsRV.setLayoutManager(new LinearLayoutManager(this));
        mLocationItemsRV.setHasFixedSize(true);


        SavedCitiesViewModel savedCitiesViewModel =
                new ViewModelProvider(
                        this,
                        new ViewModelProvider.AndroidViewModelFactory(
                                getApplication()
                        )
                ).get(SavedCitiesViewModel.class);

        LiveData<List<ForecastLocation>> allRepos =
                savedCitiesViewModel.getAllLocations();


        allRepos.observe(this, new Observer<List<ForecastLocation>>() {
            @Override
            public void
            onChanged(@Nullable List<ForecastLocation> gitHubRepos) {
                Log.d("yea", Integer.toString(gitHubRepos.size()));
                mLocationsAdapter.updateForecastItems(gitHubRepos);
            }
        });

        /*END CHANGES*/

        // Remove shadow under action bar.
        //getSupportActionBar().setElevation(0);

        mForecastLocationTV = findViewById(R.id.tv_forecast_location);
        mLoadingIndicatorPB = findViewById(R.id.pb_loading_indicator);
        mLoadingErrorMessageTV = findViewById(R.id.tv_loading_error_message);
        mForecastItemsRV = findViewById(R.id.rv_forecast_items);

        mForecastAdapter = new ForecastAdapter(this);
        mForecastItemsRV.setAdapter(mForecastAdapter);
        mForecastItemsRV.setLayoutManager(new LinearLayoutManager(this));
        mForecastItemsRV.setHasFixedSize(true);



        /*
         * This version of the app code uses the new ViewModel architecture to manage data for
         * the activity.  See the classes in the data package for more about how the ViewModel
         * is set up.  Here, we simply grab the forecast data ViewModel.
         */
        mForecastViewModel = new ViewModelProvider(this).get(ForecastViewModel.class);

        /*
         * Attach an Observer to the forecast data.  Whenever the forecast data changes, this
         * Observer will send the new data into our RecyclerView's adapter.
         */
        mForecastViewModel.getForecast().observe(this, new Observer<List<ForecastItem>>() {
            @Override
            public void onChanged(@Nullable List<ForecastItem> forecastItems) {
                mForecastAdapter.updateForecastItems(forecastItems);
            }
        });

        /*
         * Attach an Observer to the network loading status.  Whenever the loading status changes,
         * this Observer will ensure that the correct layout components are visible.  Specifically,
         * it will make the loading indicator visible only when the forecast is being loaded.
         * Otherwise, it will display the RecyclerView if forecast data was successfully fetched,
         * or it will display the error message if there was an error fetching data.
         */
        mForecastViewModel.getLoadingStatus().observe(this, new Observer<Status>() {
            @Override
            public void onChanged(@Nullable Status status) {
                if (status == Status.LOADING) {
                    mLoadingIndicatorPB.setVisibility(View.VISIBLE);
                } else if (status == Status.SUCCESS) {
                    mLoadingIndicatorPB.setVisibility(View.INVISIBLE);
                    mLoadingErrorMessageTV.setVisibility(View.INVISIBLE);
                    mForecastItemsRV.setVisibility(View.VISIBLE);
                } else {
                    mLoadingIndicatorPB.setVisibility(View.INVISIBLE);
                    mForecastItemsRV.setVisibility(View.INVISIBLE);
                    mLoadingErrorMessageTV.setVisibility(View.VISIBLE);
                }
            }
        });

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
        loadForecast(preferences);
    }

    @Override
    protected void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onForecastItemClick(ForecastItem forecastItem) {
        Intent intent = new Intent(this, ForecastItemDetailActivity.class);
        intent.putExtra(OpenWeatherMapUtils.EXTRA_FORECAST_ITEM, forecastItem);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_location:
                showForecastLocationInMap();
                return true;
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void loadForecast(SharedPreferences preferences) {
        String location = preferences.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default_value)
        );
        String units = preferences.getString(
                getString(R.string.pref_units_key),
                getString(R.string.pref_units_default_value)
        );

        mForecastLocationTV.setText(location);
        mForecastViewModel.loadForecast(location, units);
    }

    public void showForecastLocationInMap() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String forecastLocation = sharedPreferences.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default_value)
        );
        Uri geoUri = Uri.parse("geo:0,0").buildUpon()
                .appendQueryParameter("q", forecastLocation)
                .build();
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, geoUri);
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        loadForecast(sharedPreferences);
    }

// NavBar
    @Override
    public void onForecastItemClick(ForecastLocation forecastItem) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putString(getString(R.string.pref_location_key), forecastItem.loc_name).apply();
        mDrawerLayout.closeDrawers();
    }

    @Override
    public void onNavigationItemReselected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_home:
                Log.d("navBar", "reselected on home!");
//                Intent startIntent = new Intent(this, MainActivity.class);
//                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                this.startActivity(startIntent);
                showForecastLocationInMap();
            case R.id.nav_scan:
                Log.d("navBar", "reselected on scam!");
                showForecastLocationInMap();
        }
    }
// NavBar END
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_home:
                Log.d("navBar", "reselected on home!");
//                Intent startIntent = new Intent(this, MainActivity.class);
//                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                this.startActivity(startIntent);
                showForecastLocationInMap();
                return true;
            case R.id.nav_scan:
                Log.d("navBar", "reselected on scam!");
                showForecastLocationInMap();
                return true;
            default:
                return false;
        }
    }
}
