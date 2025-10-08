// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package shmutalov.verusminer9000;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import shmutalov.verusminer9000.api.PoolItem;
import shmutalov.verusminer9000.api.ProviderManager;

public class WizardPoolActivity extends BaseActivity {

    private int selectedPoolIndex = 1;
    private LinearLayout poolContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        setContentView(R.layout.fragment_wizard_pool);

        poolContainer = findViewById(R.id.poolContainer);

        // Generate pools from ProviderManager
        ProviderManager.generate();
        PoolItem[] pools = ProviderManager.getPools();

        // Skip the first "custom" pool in wizard (index 0)
        for (int i = 1; i < pools.length; i++) {
            addPoolItem(pools[i], i);
        }

        // Select first pool by default
        setHover(selectedPoolIndex);
    }

    private void addPoolItem(PoolItem pool, final int poolIndex) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View poolItemView = inflater.inflate(R.layout.item_wizard_pool, poolContainer, false);

        LinearLayout poolLayout = poolItemView.findViewById(R.id.poolItemLayout);
        TextView poolName = poolItemView.findViewById(R.id.poolName);
        TextView poolLabel = poolItemView.findViewById(R.id.poolLabel);

        // Set pool name (uppercase handled by android:textAllCaps in XML)
        poolName.setText(pool.getKey());

        // Set "recommended" label for official Verus pool
        if (poolIndex == 1) {
            poolLabel.setText(R.string.recommended);
            poolLabel.setVisibility(View.VISIBLE);
        } else {
            poolLabel.setVisibility(View.GONE);
        }

        // Set click listener
        poolLayout.setOnClickListener(v -> {
            selectedPoolIndex = poolIndex;
            setHover(poolIndex);
        });

        // Store pool index as tag for later reference
        poolLayout.setTag(poolIndex);

        // Add first pool with top margin (already in item_wizard_pool.xml for others)
        if (poolIndex == 1) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) poolItemView.getLayoutParams();
            if (params == null) {
                params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
            }
            int topMargin = (int) (15 * getResources().getDisplayMetrics().density); // 15dp
            params.setMargins(0, topMargin, 0, 0);
            poolItemView.setLayoutParams(params);

            // First pool selected by default
            poolLayout.setBackgroundResource(R.drawable.corner_radius_lighter_border_blue);
        }

        poolContainer.addView(poolItemView);
    }

    private void setHover(int poolIndex) {
        // Update all pool items
        for (int i = 0; i < poolContainer.getChildCount(); i++) {
            View child = poolContainer.getChildAt(i);
            LinearLayout poolLayout = child.findViewById(R.id.poolItemLayout);

            if (poolLayout != null) {
                Integer tag = (Integer) poolLayout.getTag();
                if (tag != null && tag == poolIndex) {
                    poolLayout.setBackgroundResource(R.drawable.corner_radius_lighter_border_blue);
                } else {
                    poolLayout.setBackgroundResource(R.drawable.corner_radius_lighter);
                }
            }
        }
    }

    public void onNext(View view) {
        Config.write("selected_pool", Integer.toString(selectedPoolIndex));

        startActivity(new Intent(WizardPoolActivity.this, WizardSettingsActivity.class));
        finish();
    }

    public void onSkip(View view) {
        Config.write("selected_pool", "0");

        startActivity(new Intent(WizardPoolActivity.this, WizardSettingsActivity.class));
        finish();
    }
}