package adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class CountryAdapter extends ArrayAdapter<String> {
    private final List<String> countries;
    private OnItemSelectedListener listener;

    public interface OnItemSelectedListener {
        void onItemSelected(String country);
        void onNothingSelected();
    }

    public CountryAdapter(@NonNull Context context, List<String> countries) {
        super(context, android.R.layout.simple_spinner_item, countries);
        this.countries = countries;
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        if (listener != null) {
            listener.onItemSelected(countries.get(position));
        }
        return view;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        return view;
    }
} 