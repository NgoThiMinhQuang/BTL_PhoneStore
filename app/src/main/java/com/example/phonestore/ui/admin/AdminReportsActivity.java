package com.example.phonestore.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.OrderDao;
import com.example.phonestore.data.dao.UserDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.ui.home.BaseHomeActivity;
import com.example.phonestore.ui.orders.OrdersAdapter;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AdminReportsActivity extends BaseHomeActivity {
    private static final int PROFIT_RATE_PERCENT = 18;

    private OrderDao orderDao;
    private UserDao userDao;

    @Override
    protected int shellLayoutRes() {
        return R.layout.activity_admin_reports;
    }

    @Override
    protected int contentLayoutRes() {
        return 0;
    }

    @Override
    protected int bottomMenuRes() {
        return R.menu.menu_bottom_admin;
    }

    @Override
    protected int selectedBottomNavItemId() {
        return R.id.nav_admin_more;
    }

    @Override
    protected String screenTitle() {
        return getString(R.string.admin_reports_title);
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    @Override
    protected boolean shouldShowBackButton() {
        return true;
    }

    @Override
    protected boolean shouldUseAdminBackButtonStyling() {
        return true;
    }

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
        userDao = new UserDao(this);

        int year = Calendar.getInstance().get(Calendar.YEAR);
        OrderDao.ReportMetrics metrics = orderDao.getReportMetrics();
        ArrayList<OrderDao.MonthRevenue> revenueByMonth = orderDao.getDoanhThuTheoThang(year);
        ArrayList<OrderDao.MonthCount> ordersByMonth = orderDao.getSoDonTheoThang(year);
        ArrayList<OrderDao.ProductSale> topProducts = orderDao.getTopSanPhamBanChay(4);
        ArrayList<OrderDao.StatusCount> orderStatuses = orderDao.getSoDonTheoTrangThai();

        bindHeadlineKpis(metrics);
        bindLegacyKpis(metrics);
        bindSummary(metrics, revenueByMonth, ordersByMonth);

        LineChart chartRevenue = findViewById(R.id.chartRevenue);
        BarChart chartTopProducts = findViewById(R.id.chartTopProducts);
        PieChart chartOrderStatus = findViewById(R.id.chartOrderStatus);
        BarChart chartRevenueProfit = findViewById(R.id.chartRevenueProfit);
        PieChart chartBestSellers = findViewById(R.id.chartBestSellers);

        renderRevenueByMonth(chartRevenue, revenueByMonth, year);
        renderTopProducts(chartTopProducts, topProducts);
        renderOrderStatus(chartOrderStatus, orderStatuses);
        renderRevenueProfit(chartRevenueProfit, revenueByMonth);
        renderBestSellers(chartBestSellers, topProducts);
    }

    private void bindHeadlineKpis(OrderDao.ReportMetrics metrics) {
        View cardRevenue = findViewById(R.id.cardRevenue);
        View cardProfit = findViewById(R.id.cardProfit);
        View cardUnits = findViewById(R.id.cardUnitsSold);
        View cardOrders = findViewById(R.id.cardOrders);

        bindKpiCard(cardRevenue, R.string.admin_reports_kpi_delivered_revenue, formatMoneyShort(metrics.recognizedRevenue));
        bindKpiCard(cardProfit, R.string.admin_reports_kpi_profit, formatMoneyShort(calculateProfit(metrics.recognizedRevenue)));
        bindKpiCard(cardUnits, R.string.admin_reports_kpi_units, String.valueOf(metrics.deliveredUnits));
        bindKpiCard(cardOrders, R.string.admin_reports_kpi_orders, String.valueOf(metrics.totalOrders));
    }

    private void bindLegacyKpis(OrderDao.ReportMetrics metrics) {
        TextView tvRevenue = findViewById(R.id.tvRevenue);
        TextView tvOrders = findViewById(R.id.tvOrders);
        TextView tvCustomers = findViewById(R.id.tvCustomers);
        if (tvRevenue != null) {
            tvRevenue.setText(getString(R.string.kpi_revenue_value, formatMoney(metrics.recognizedRevenue)));
        }
        if (tvOrders != null) {
            tvOrders.setText(getString(R.string.kpi_orders_value, metrics.totalOrders));
        }
        if (tvCustomers != null) {
            tvCustomers.setText(getString(R.string.kpi_customers_value, userDao.getSoKhachHang()));
        }
    }

    private void bindSummary(OrderDao.ReportMetrics metrics,
                             ArrayList<OrderDao.MonthRevenue> revenueByMonth,
                             ArrayList<OrderDao.MonthCount> ordersByMonth) {
        int totalRevenue6m = sumLatestRevenue(revenueByMonth, 6);
        int totalProfit6m = calculateProfit(totalRevenue6m);
        int totalOrders6m = sumLatestOrders(ordersByMonth, 6);
        int averageOrderValue = metrics.totalOrders == 0 ? 0 : metrics.recognizedRevenue / metrics.totalOrders;

        ((TextView) findViewById(R.id.tvReportTotalRevenue6m)).setText(formatMoneyCompact(totalRevenue6m));
        ((TextView) findViewById(R.id.tvReportTotalProfit6m)).setText(formatMoneyCompact(totalProfit6m));
        ((TextView) findViewById(R.id.tvReportTotalOrders)).setText(String.valueOf(totalOrders6m));
        ((TextView) findViewById(R.id.tvReportAverageOrder)).setText(formatMoneyShort(averageOrderValue));
    }

    private void renderRevenueByMonth(LineChart chart, ArrayList<OrderDao.MonthRevenue> raw, int year) {
        chart.setNoDataText(getString(R.string.admin_reports_empty_revenue));
        chart.getDescription().setEnabled(false);

        int[] rev = new int[13];
        for (OrderDao.MonthRevenue r : raw) {
            if (r.month >= 1 && r.month <= 12) rev[r.month] = r.revenue;
        }

        ArrayList<Entry> entries = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            entries.add(new Entry(m, rev[m]));
        }

        LineDataSet set = new LineDataSet(entries, getString(R.string.admin_reports_delivered_revenue_label, year));
        set.setLineWidth(2.5f);
        set.setCircleRadius(3.5f);
        set.setValueTextSize(10f);
        set.setColor(ContextCompat.getColor(this, R.color.admin_primary));
        set.setCircleColor(ContextCompat.getColor(this, R.color.admin_primary));
        set.setDrawFilled(true);
        set.setFillColor(ContextCompat.getColor(this, R.color.admin_surface_soft));

        LineData data = new LineData(set);
        chart.setData(data);
        chart.getAxisRight().setEnabled(false);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);
        x.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int m = (int) value;
                return (m >= 1 && m <= 12) ? "T" + m : "";
            }
        });

        chart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatAxisMillions((int) value);
            }
        });

        chart.invalidate();
    }

    private void renderTopProducts(BarChart chart, ArrayList<OrderDao.ProductSale> list) {
        chart.setNoDataText(getString(R.string.admin_reports_empty_top_products));
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            OrderDao.ProductSale p = list.get(i);
            entries.add(new BarEntry(i, p.qty));
            labels.add(shortName(p.name));
        }

        BarDataSet set = new BarDataSet(entries, getString(R.string.admin_reports_top_products_label));
        set.setColor(ContextCompat.getColor(this, R.color.admin_primary));
        set.setValueTextSize(10f);

        BarData data = new BarData(set);
        data.setBarWidth(0.9f);

        chart.setData(data);
        chart.setFitBars(true);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setLabelRotationAngle(-30f);

        chart.invalidate();
    }

    private void renderOrderStatus(PieChart chart, ArrayList<OrderDao.StatusCount> list) {
        chart.setNoDataText(getString(R.string.admin_reports_empty_status));
        chart.getDescription().setEnabled(false);
        chart.setDrawEntryLabels(false);

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (OrderDao.StatusCount s : list) {
            entries.add(new PieEntry(s.count, OrdersAdapter.formatOrderStatus(this, s.status)));
        }

        PieDataSet set = new PieDataSet(entries, getString(R.string.admin_reports_status_label));
        set.setColors(
                ContextCompat.getColor(this, R.color.admin_primary),
                ContextCompat.getColor(this, R.color.admin_warning),
                ContextCompat.getColor(this, R.color.admin_success),
                ContextCompat.getColor(this, R.color.admin_danger)
        );
        set.setValueTextSize(11f);

        PieData data = new PieData(set);
        chart.setData(data);
        chart.invalidate();
    }

    private void renderRevenueProfit(BarChart chart, ArrayList<OrderDao.MonthRevenue> revenueByMonth) {
        chart.setNoDataText(getString(R.string.admin_reports_empty_revenue));
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setFitBars(true);

        int[] revenue = new int[7];
        for (OrderDao.MonthRevenue item : revenueByMonth) {
            if (item.month >= 1 && item.month <= 6) {
                revenue[item.month] = item.revenue;
            }
        }

        ArrayList<BarEntry> revenueEntries = new ArrayList<>();
        ArrayList<BarEntry> profitEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        for (int month = 1; month <= 6; month++) {
            revenueEntries.add(new BarEntry(month - 1, toMillions(revenue[month])));
            profitEntries.add(new BarEntry(month - 1, toMillions(calculateProfit(revenue[month]))));
            labels.add("T" + month);
        }

        BarDataSet revenueSet = new BarDataSet(revenueEntries, getString(R.string.admin_reports_chart_revenue_label));
        revenueSet.setColor(ContextCompat.getColor(this, R.color.admin_dashboard_blue));
        BarDataSet profitSet = new BarDataSet(profitEntries, getString(R.string.admin_reports_chart_profit_label));
        profitSet.setColor(ContextCompat.getColor(this, R.color.admin_dashboard_green));
        revenueSet.setValueTextSize(10f);
        profitSet.setValueTextSize(10f);

        BarData data = new BarData(revenueSet, profitSet);
        data.setBarWidth(0.28f);
        chart.setData(data);
        chart.groupBars(-0.5f, 0.24f, 0.08f);
        chart.getXAxis().setAxisMinimum(-0.5f);
        chart.getXAxis().setAxisMaximum(5.5f + 1f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(12f);
        chart.invalidate();
    }

    private void renderBestSellers(PieChart chart, ArrayList<OrderDao.ProductSale> topProducts) {
        chart.setNoDataText(getString(R.string.admin_reports_empty_top_products));
        chart.getDescription().setEnabled(false);
        chart.setUsePercentValues(true);
        chart.setDrawEntryLabels(false);
        chart.setCenterText("");

        ArrayList<PieEntry> entries = new ArrayList<>();
        int totalQty = 0;
        for (OrderDao.ProductSale product : topProducts) {
            totalQty += Math.max(0, product.qty);
        }
        if (totalQty <= 0) {
            chart.clear();
            chart.invalidate();
            return;
        }

        int shownQty = 0;
        for (int i = 0; i < topProducts.size() && i < 3; i++) {
            OrderDao.ProductSale product = topProducts.get(i);
            shownQty += Math.max(0, product.qty);
            entries.add(new PieEntry(product.qty, shortName(product.name)));
        }
        int otherQty = Math.max(0, totalQty - shownQty);
        if (otherQty > 0) {
            entries.add(new PieEntry(otherQty, "Khác"));
        }

        PieDataSet set = new PieDataSet(entries, getString(R.string.admin_reports_best_sellers_title));
        set.setSliceSpace(2f);
        set.setSelectionShift(4f);
        set.setColors(
                ContextCompat.getColor(this, R.color.admin_dashboard_blue),
                ContextCompat.getColor(this, R.color.admin_dashboard_green),
                ContextCompat.getColor(this, R.color.admin_dashboard_orange),
                ContextCompat.getColor(this, R.color.admin_dashboard_purple)
        );
        PieData data = new PieData(set);
        data.setValueFormatter(new PercentFormatter(chart));
        data.setValueTextSize(12f);
        chart.setData(data);
        chart.invalidate();
    }

    private void bindKpiCard(View card, int labelRes, String value) {
        if (card == null) {
            return;
        }
        ((TextView) card.findViewById(R.id.tvKpiLabel)).setText(labelRes);
        ((TextView) card.findViewById(R.id.tvKpiValue)).setText(value);
    }

    private int sumLatestRevenue(ArrayList<OrderDao.MonthRevenue> revenues, int maxMonth) {
        int total = 0;
        for (OrderDao.MonthRevenue item : revenues) {
            if (item.month >= 1 && item.month <= maxMonth) {
                total += Math.max(0, item.revenue);
            }
        }
        return total;
    }

    private int sumLatestOrders(ArrayList<OrderDao.MonthCount> orders, int maxMonth) {
        int total = 0;
        for (OrderDao.MonthCount item : orders) {
            if (item.month >= 1 && item.month <= maxMonth) {
                total += Math.max(0, item.count);
            }
        }
        return total;
    }

    private int calculateProfit(int revenue) {
        return Math.max(0, revenue * PROFIT_RATE_PERCENT / 100);
    }

    private float toMillions(int amount) {
        return amount / 1_000_000f;
    }

    private String formatAxisMillions(int amount) {
        return String.valueOf((int) Math.round(toMillions(amount)));
    }

    private String formatMoney(int amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + "đ";
    }

    private String formatMoneyShort(int amount) {
        if (amount >= 1_000_000_000) {
            return trimDecimal(amount / 1_000_000_000f) + "B";
        }
        if (amount >= 1_000_000) {
            return trimDecimal(amount / 1_000_000f) + "M";
        }
        if (amount >= 1_000) {
            return trimDecimal(amount / 1_000f) + "K";
        }
        return String.valueOf(amount);
    }

    private String formatMoneyCompact(int amount) {
        if (amount >= 1_000_000_000) {
            return trimDecimal(amount / 1_000_000_000f) + " tỷ";
        }
        if (amount >= 1_000_000) {
            return trimDecimal(amount / 1_000_000f) + " triệu";
        }
        return formatMoney(amount);
    }

    private String trimDecimal(float value) {
        if (Math.abs(value - Math.round(value)) < 0.05f) {
            return String.valueOf(Math.round(value));
        }
        return String.format(Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private String shortName(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() <= 14) return s;
        return s.substring(0, 14) + "…";
    }
}
