package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.OrderDao;
import com.example.phonestore.data.dao.ProductDao;
import com.example.phonestore.data.dao.UserDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Order;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.ui.admin.AdminProductsActivity;
import com.example.phonestore.ui.admin.AdminReportsActivity;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.ui.orders.OrderDetailActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdminHomeActivity extends BaseHomeActivity {

    private OrderDao orderDao;
    private ProductDao productDao;
    private UserDao userDao;
    private final NumberFormat moneyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!session.isLoggedIn() || !DBHelper.ROLE_ADMIN.equals(session.getRole())) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        orderDao = new OrderDao(this);
        productDao = new ProductDao(this);
        userDao = new UserDao(this);
    }

    @Override
    protected int shellLayoutRes() {
        return R.layout.activity_home_bottom_admin;
    }

    @Override
    protected int contentLayoutRes() {
        return R.layout.content_admin_dashboard;
    }

    @Override
    protected int bottomMenuRes() {
        return R.menu.menu_bottom_admin;
    }

    @Override
    protected String screenTitle() {
        return getString(R.string.admin_dashboard);
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    @Override
    protected void onShellReady() {
        if (orderDao == null || productDao == null || userDao == null) return;

        ArrayList<Order> orders = orderDao.getAllOrders();
        ArrayList<Product> products = productDao.layTatCa();
        ArrayList<OrderDao.MonthRevenue> revenueByMonth = orderDao.getDoanhThuTheoThang(Calendar.getInstance().get(Calendar.YEAR));

        bindKpis(orders, products, revenueByMonth);
        bindRevenueChart(revenueByMonth);
        bindQuickActions();
        bindRecentOrders(orders);
    }

    private void bindKpis(ArrayList<Order> orders, ArrayList<Product> products, ArrayList<OrderDao.MonthRevenue> revenueByMonth) {
        TextView tvRevenueMonth = findViewById(R.id.tvRevenueMonth);
        TextView tvOrdersCount = findViewById(R.id.tvOrdersCount);
        TextView tvCustomersCount = findViewById(R.id.tvCustomersCount);
        TextView tvProductsCount = findViewById(R.id.tvProductsCount);
        TextView tvLowStockSummary = findViewById(R.id.tvLowStockSummary);

        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int monthRevenue = 0;
        for (OrderDao.MonthRevenue item : revenueByMonth) {
            if (item.month == currentMonth) {
                monthRevenue = item.revenue;
                break;
            }
        }

        int lowStockCount = 0;
        List<String> highlightNames = new ArrayList<>();
        for (Product product : products) {
            if (product.tonKho > 0 && product.tonKho <= 5) {
                lowStockCount++;
                if (highlightNames.size() < 2) {
                    highlightNames.add(product.tenSanPham);
                }
            }
        }

        tvRevenueMonth.setText(getString(R.string.admin_price_currency, moneyFormat.format(monthRevenue)));
        tvOrdersCount.setText(String.valueOf(orders.size()));
        tvCustomersCount.setText(String.valueOf(userDao.getSoKhachHang()));
        tvProductsCount.setText(String.valueOf(products.size()));

        if (lowStockCount == 0) {
            tvLowStockSummary.setText(R.string.admin_no_low_stock);
        } else {
            String summary = getString(R.string.admin_low_stock_summary, lowStockCount);
            if (!highlightNames.isEmpty()) {
                summary += " • " + joinNames(highlightNames);
            }
            tvLowStockSummary.setText(summary);
        }
    }

    private void bindRevenueChart(ArrayList<OrderDao.MonthRevenue> revenueByMonth) {
        LineChart chart = findViewById(R.id.chartRevenueMini);
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setNoDataText("Chưa có dữ liệu doanh thu");
        chart.setExtraLeftOffset(4f);
        chart.setExtraRightOffset(4f);

        int[] revenue = new int[13];
        for (OrderDao.MonthRevenue item : revenueByMonth) {
            if (item.month >= 1 && item.month <= 12) {
                revenue[item.month] = item.revenue;
            }
        }

        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int startMonth = Math.max(1, currentMonth - 5);
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<Integer> labels = new ArrayList<>();
        for (int month = startMonth; month <= currentMonth; month++) {
            entries.add(new Entry(month, revenue[month]));
            labels.add(month);
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.admin_revenue_chart_title));
        dataSet.setColor(ContextCompat.getColor(this, R.color.admin_primary));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.admin_primary));
        dataSet.setCircleHoleColor(ContextCompat.getColor(this, R.color.admin_surface));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.admin_surface_soft));

        chart.setData(new LineData(dataSet));
        chart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return moneyFormat.format((int) value);
            }
        });

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int month = (int) value;
                return "T" + month;
            }
        });

        chart.invalidate();
    }

    private void bindQuickActions() {
        View btnQuickAddProduct = findViewById(R.id.btnQuickAddProduct);
        View btnQuickInventory = findViewById(R.id.btnQuickInventory);
        View btnQuickOrders = findViewById(R.id.btnQuickOrders);
        View btnQuickReports = findViewById(R.id.btnQuickReports);

        btnQuickAddProduct.setOnClickListener(v -> openBottomTab(new Intent(this, AdminProductsActivity.class)));
        btnQuickInventory.setOnClickListener(v -> openBottomTab(new Intent(this, AdminInventoryActivity.class)));
        btnQuickOrders.setOnClickListener(v -> openBottomTab(new Intent(this, AdminOrdersActivity.class)));
        btnQuickReports.setOnClickListener(v -> startActivity(new Intent(this, AdminReportsActivity.class)));
    }

    private void bindRecentOrders(ArrayList<Order> orders) {
        LinearLayout container = findViewById(R.id.layoutRecentOrders);
        container.removeAllViews();

        if (orders.isEmpty()) {
            addDashboardEntry(container, getString(R.string.admin_no_recent_orders), "", "").setClickable(false);
            return;
        }

        int limit = Math.min(4, orders.size());
        for (int i = 0; i < limit; i++) {
            Order order = orders.get(i);
            String title = order.username == null
                    ? getString(R.string.admin_order_code, order.id)
                    : getString(R.string.admin_order_code_with_user, order.id, order.username);
            String sub = formatStatus(order.trangThai);
            String meta = getString(R.string.admin_price_currency, moneyFormat.format(order.tongTien));
            View itemView = addDashboardEntry(container, title, sub, meta);
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(this, OrderDetailActivity.class);
                intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.id);
                startActivity(intent);
            });
        }
    }

    private View addDashboardEntry(LinearLayout container, String title, String sub, String meta) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_admin_dashboard_entry, container, false);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvSub = view.findViewById(R.id.tvSub);
        TextView tvMeta = view.findViewById(R.id.tvMeta);
        tvTitle.setText(title);
        tvSub.setText(sub);
        tvMeta.setText(meta);
        container.addView(view);
        return view;
    }

    private String formatStatus(String status) {
        if (status == null) return "";
        return status.replace('_', ' ');
    }

    private String joinNames(List<String> names) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) builder.append(" • ");
            builder.append(names.get(i));
        }
        return builder.toString();
    }
}
