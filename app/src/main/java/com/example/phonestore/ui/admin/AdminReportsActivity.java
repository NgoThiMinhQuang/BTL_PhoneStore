package com.example.phonestore.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import android.view.View;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.OrderDao;
import com.example.phonestore.data.dao.UserDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.utils.SessionManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
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
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AdminReportsActivity extends AppCompatActivity {

    private SessionManager session;
    private OrderDao orderDao;
    private UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reports);

        session = new SessionManager(this);
        if (!session.isLoggedIn() || !DBHelper.ROLE_ADMIN.equals(session.getRole())) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        orderDao = new OrderDao(this);
        userDao = new UserDao(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.admin_reports_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // KPI
        TextView tvRevenue = findViewById(R.id.tvRevenue);
        TextView tvOrders = findViewById(R.id.tvOrders);
        TextView tvCustomers = findViewById(R.id.tvCustomers);
        View cardRevenue = findViewById(R.id.cardRevenue);
        View cardOrders = findViewById(R.id.cardOrders);
        View cardCustomers = findViewById(R.id.cardCustomers);

        String revenue = NumberFormat.getNumberInstance(new Locale("vi", "VN"))
                .format(orderDao.getTongDoanhThu()) + "đ";

        tvRevenue.setText(getString(R.string.kpi_revenue_value, revenue));
        tvOrders.setText(getString(R.string.kpi_orders_value, orderDao.getSoDonHang()));
        tvCustomers.setText(getString(R.string.kpi_customers_value, userDao.getSoKhachHang()));

        ((TextView) cardRevenue.findViewById(R.id.tvKpiLabel)).setText(R.string.admin_revenue_month);
        ((TextView) cardRevenue.findViewById(R.id.tvKpiValue)).setText(revenue);
        ((TextView) cardOrders.findViewById(R.id.tvKpiLabel)).setText(R.string.admin_total_orders);
        ((TextView) cardOrders.findViewById(R.id.tvKpiValue)).setText(String.valueOf(orderDao.getSoDonHang()));
        ((TextView) cardCustomers.findViewById(R.id.tvKpiLabel)).setText(R.string.admin_total_customers);
        ((TextView) cardCustomers.findViewById(R.id.tvKpiValue)).setText(String.valueOf(userDao.getSoKhachHang()));

        // Charts
        LineChart chartRevenue = findViewById(R.id.chartRevenue);
        BarChart chartTopProducts = findViewById(R.id.chartTopProducts);
        PieChart chartOrderStatus = findViewById(R.id.chartOrderStatus);

        int year = Calendar.getInstance().get(Calendar.YEAR);

        renderRevenueByMonth(chartRevenue, orderDao.getDoanhThuTheoThang(year), year);
        renderTopProducts(chartTopProducts, orderDao.getTopSanPhamBanChay(5));
        renderOrderStatus(chartOrderStatus, orderDao.getSoDonTheoTrangThai());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void renderRevenueByMonth(LineChart chart, ArrayList<OrderDao.MonthRevenue> raw, int year) {
        chart.setNoDataText("Chưa có dữ liệu doanh thu");
        chart.getDescription().setEnabled(false);

        // fill đủ 12 tháng
        int[] rev = new int[13]; // 1..12
        for (OrderDao.MonthRevenue r : raw) {
            if (r.month >= 1 && r.month <= 12) rev[r.month] = r.revenue;
        }

        ArrayList<Entry> entries = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            entries.add(new Entry(m, rev[m]));
        }

        LineDataSet set = new LineDataSet(entries, "Doanh thu " + year);
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

        // format tiền bên trục Y
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        chart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return nf.format((int) value);
            }
        });

        chart.invalidate();
    }

    private void renderTopProducts(BarChart chart, ArrayList<OrderDao.ProductSale> list) {
        chart.setNoDataText("Chưa có dữ liệu bán hàng");
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            OrderDao.ProductSale p = list.get(i);
            entries.add(new BarEntry(i, p.qty));
            labels.add(shortName(p.name));
        }

        BarDataSet set = new BarDataSet(entries, "Số lượng bán");
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
        chart.setNoDataText("Chưa có dữ liệu trạng thái đơn");
        chart.getDescription().setEnabled(false);
        chart.setDrawEntryLabels(false);

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (OrderDao.StatusCount s : list) {
            entries.add(new PieEntry(s.count, s.status));
        }

        PieDataSet set = new PieDataSet(entries, "Trạng thái");
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

    private String shortName(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() <= 12) return s;
        return s.substring(0, 12) + "…";
    }
}