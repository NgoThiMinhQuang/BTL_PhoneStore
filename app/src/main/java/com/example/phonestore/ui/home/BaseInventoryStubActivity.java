package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.phonestore.R;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.ui.auth.WelcomeActivity;

public abstract class BaseInventoryStubActivity extends BaseHomeActivity {

    protected static class StubKpi {
        final String label;
        final String value;

        StubKpi(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!session.isLoggedIn() || !DBHelper.ROLE_ADMIN.equals(session.getRole())) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        }
    }

    @Override
    protected int shellLayoutRes() {
        return R.layout.activity_home_bottom_admin;
    }

    @Override
    protected int contentLayoutRes() {
        return R.layout.activity_admin_inventory_stub;
    }

    @Override
    protected int bottomMenuRes() {
        return R.menu.menu_bottom_admin;
    }

    @Override
    protected int selectedBottomNavItemId() {
        return R.id.nav_admin_inventory;
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    protected abstract String stubTitle();

    protected abstract String stubSummary();

    protected StubKpi primaryKpi() {
        return new StubKpi(getString(R.string.admin_kpi_primary_label), "--");
    }

    protected StubKpi secondaryKpi() {
        return new StubKpi(getString(R.string.admin_kpi_secondary_label), "--");
    }

    protected void populateStubContent(LinearLayout layoutStubContent) {
        addEntry(layoutStubContent, getString(R.string.admin_feature_coming_soon), getString(R.string.admin_stub_message), "");
    }

    protected View addEntry(LinearLayout container, String title, String sub, String meta) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_admin_dashboard_entry, container, false);
        ((TextView) view.findViewById(R.id.tvTitle)).setText(title);
        ((TextView) view.findViewById(R.id.tvSub)).setText(sub);
        ((TextView) view.findViewById(R.id.tvMeta)).setText(meta);
        container.addView(view);
        return view;
    }

    private void bindKpi(int cardId, StubKpi kpi) {
        View card = findViewById(cardId);
        TextView tvLabel = card.findViewById(R.id.tvKpiLabel);
        TextView tvValue = card.findViewById(R.id.tvKpiValue);
        tvLabel.setText(kpi.label);
        tvValue.setText(kpi.value);
    }

    @Override
    protected void onShellReady() {
        TextView tvStubTitle = findViewById(R.id.tvStubTitle);
        LinearLayout layoutStubContent = findViewById(R.id.layoutStubContent);

        tvStubTitle.setText(stubTitle());
        bindKpi(R.id.cardStubKpiPrimary, primaryKpi());
        bindKpi(R.id.cardStubKpiSecondary, secondaryKpi());

        layoutStubContent.removeAllViews();
        addEntry(layoutStubContent, stubTitle(), stubSummary(), "");
        populateStubContent(layoutStubContent);
    }
}
