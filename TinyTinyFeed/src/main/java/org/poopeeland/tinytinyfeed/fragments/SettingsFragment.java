package org.poopeeland.tinytinyfeed.fragments;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import org.poopeeland.tinytinyfeed.utils.ExceptionAsyncTask;
import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.widgets.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.models.Category;
import org.poopeeland.tinytinyfeed.network.exceptions.FetchException;
import org.poopeeland.tinytinyfeed.network.Fetcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * A simple {@link PreferenceFragment} subclass.
 * <p>
 * Tries to load the categories created on tt-rss when one or several widgets are on screen.
 */
public class SettingsFragment extends PreferenceFragment {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        PreferenceScreen screen = getPreferenceScreen();
        Resources res = getResources();

        AppWidgetManager manager = AppWidgetManager.getInstance(screen.getContext());
        TinyTinyFeedWidget.class.getPackage();

        int[] ids = manager.getAppWidgetIds(new ComponentName(screen.getContext().getPackageName(),
                TinyTinyFeedWidget.class.getName()));

        SharedPreferences preferences = screen.getSharedPreferences();
        if (ids.length > 0 && preferences.getBoolean(TinyTinyFeedWidget.CHECKED, false)) {
            try {
                Log.d(TAG, "Fetching categories...");
                AsyncCategoryFetcher fetcher = new AsyncCategoryFetcher(preferences, ids, res, screen);
                fetcher.execute();
            } catch (FetchException e) {
                Log.e(TAG, "Exception while creating the fetcher", e);
            }

        }
    }

    private void addPreferenceList(final int id,
                                   final PreferenceCategory category,
                                   final Resources res,
                                   final PreferenceScreen screen,
                                   final CharSequence[] entries,
                                   final CharSequence[] entryValues) {
        String preferenceKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.WIDGET_CATEGORIES_KEY, id);
        MultiSelectListPreference p = new MultiSelectListPreference(screen.getContext());
        p.setKey(preferenceKey);
        p.setTitle(res.getString(R.string.widget_categories_title, id));
        p.setSummary(res.getString(R.string.widget_categories_summary, id));
        p.setEntries(entries);
        p.setEntryValues(entryValues);
        SharedPreferences preferences = screen.getSharedPreferences();
        if (!preferences.contains(preferenceKey)) {
            Set<String> values = new HashSet<>();
            for (int i = 0; i < entryValues.length - 3; i++) {
                values.add(entryValues[i].toString());
            }
            p.setValues(values);
            preferences.edit().putStringSet(preferenceKey, values).apply();
        }
        category.addPreference(p);
    }

    private class AsyncCategoryFetcher extends ExceptionAsyncTask<Void, Void, List<Category>> {

        private final Context context;
        private final int[] ids;
        private final Resources res;
        private final PreferenceScreen screen;
        private final Fetcher fetcher;

        private AsyncCategoryFetcher(final SharedPreferences preferences,
                                    final int[] ids,
                                    final Resources res,
                                    final PreferenceScreen screen) throws FetchException {
            super(screen.getContext());
            this.context = screen.getContext();
            this.ids = ids;
            this.res = res;
            this.screen = screen;
            this.fetcher = new Fetcher(preferences, this.context);
        }

        @Override
        protected List<Category> doInBackground() throws FetchException {
            return fetcher.fetchCategories();
        }

        @Override
        protected void onSafePostExecute(final List<Category> categories) {
            if (onError()) {
                return;
            }
            PreferenceCategory category = new PreferenceCategory(screen.getContext());
            category.setTitle(R.string.choose_category_title);
            screen.addPreference(category);

            final List<CharSequence> entriesList = new ArrayList<>();
            final List<CharSequence> entryValuesList = new ArrayList<>();

            for (Category c : categories) {
                entryValuesList.add(c.getId());
                entriesList.add(c.getTitle());
            }
            final CharSequence[] entries = entriesList.toArray(new CharSequence[entriesList.size()]);
            final CharSequence[] entryValues = entryValuesList.toArray(new CharSequence[entryValuesList.size()]);

            for (int i : ids) {
                addPreferenceList(i, category, res, screen, entries, entryValues);
            }

        }
    }

}