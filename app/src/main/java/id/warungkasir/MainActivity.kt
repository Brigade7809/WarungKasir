package id.warungkasir

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.io.OutputStream
import java.util.UUID

data class Product(val id: Long, val name: String, val price: Long, val cost: Long, val stock: Int)
class StoreDb(a: Activity) : SQLiteOpenHelper(a, "warung.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,price INTEGER NOT NULL,cost INTEGER NOT NULL,stock INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY AUTOINCREMENT,created TEXT,total INTEGER NOT NULL,paid INTEGER NOT NULL,change_amount INTEGER NOT NULL,profit INTEGER NOT NULL,items TEXT NOT NULL)")
        db.execSQL("INSERT INTO products(name,price,cost,stock) VALUES('Mi instan',3500,2600,30),('Air mineral',4000,2500,24),('Kopi sachet',2000,1200,50),('Gula 1 kg',18000,15000,10)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun products(): List<Product> {
        val out=mutableListOf<Product>(); readableDatabase.rawQuery("SELECT id,name,price,cost,stock FROM products ORDER BY name",null).use { c -> while(c.moveToNext()) out.add(Product(c.getLong(0),c.getString(1),c.getLong(2),c.getLong(3),c.getInt(4))) }; return out
    }
    fun save(name:String, price:Long, cost:Long, stock:Int, id:Long=0) {
        if(id==0L) writableDatabase.execSQL("INSERT INTO products(name,price,cost,stock) VALUES(?,?,?,?)", arrayOf(name,price,cost,stock))
        else writableDatabase.execSQL("UPDATE products SET name=?,price=?,cost=?,stock=? WHERE id=?", arrayOf(name,price,cost,stock,id))
    }
    fun remove(id:Long) { writableDatabase.delete("products","id=?", arrayOf(id.toString())) }
    fun checkout(lines:Map<Long,Int>, paid:Long): String {
        val db=writableDatabase; var total=0L; var cost=0L; val details=StringBuilder()
        db.beginTransaction()
        try {
            for((id,q) in lines) {
                val c=db.rawQuery("SELECT name,price,cost,stock FROM products WHERE id=?", arrayOf(id.toString()))
                if(!c.moveToFirst()) { c.close(); continue }
                val name=c.getString(0); val p=c.getLong(1); val co=c.getLong(2); val st=c.getInt(3); c.close()
                if(q>st) throw Exception("Stok $name tidak cukup (tersisa $st)")
                total+=p*q; cost+=co*q
                db.execSQL("UPDATE products SET stock=stock-? WHERE id=?", arrayOf(q,id))
                details.append("$name x$q = ${p*q}\n")
            }
            if(lines.isEmpty()) throw Exception("Keranjang masih kosong")
            if(paid<total) throw Exception("Uang pembayaran kurang")
            val now=SimpleDateFormat("dd-MM-yyyy HH:mm",Locale("id","ID")).format(Date())
            db.execSQL("INSERT INTO sales(created,total,paid,change_amount,profit,items) VALUES(?,?,?,?,?,?)", arrayOf(now,total,paid,paid-total,total-cost,details.toString()))
            db.setTransactionSuccessful()
            return "WARUNG KASIR\n$now\n------------------------\n$details------------------------\nTOTAL: ${rupiah(total)}\nBAYAR: ${rupiah(paid)}\nKEMBALI: ${rupiah(paid-total)}\n\nTerima kasih\n"
        } finally { db.endTransaction() }
    }
    fun report(): String {
        var total=0L; var profit=0L; var count=0
        readableDatabase.rawQuery("SELECT total,profit FROM sales WHERE substr(created,1,10)=?", arrayOf(SimpleDateFormat("dd-MM-yyyy",Locale.US).format(Date()))).use { c -> while(c.moveToNext()){total+=c.getLong(0);profit+=c.getLong(1);count++} }
        return "LAPORAN HARI INI\nTransaksi: $count\nOmzet: ${rupiah(total)}\nPerkiraan laba: ${rupiah(profit)}"
    }
    fun history(): String {
        val s=StringBuilder("RIWAYAT TRANSAKSI\n\n")
        readableDatabase.rawQuery("SELECT created,total,paid,change_amount FROM sales ORDER BY id DESC LIMIT 100",null).use { c -> while(c.moveToNext()) s.append("${c.getString(0)}\nTotal ${rupiah(c.getLong(1))} | Bayar ${rupiah(c.getLong(2))} | Kembali ${rupiah(c.getLong(3))}\n\n") }
        return s.toString()
    }
}
fun rupiah(v:Long)=NumberFormat.getCurrencyInstance(Locale("id","ID")).format(v).replace(",00","")

class MainActivity : Activity() {
    private lateinit var db: StoreDb
    private lateinit var root: LinearLayout
    private val cart=linkedMapOf<Long,Int>()
    private var lastReceipt=""
    private var lastTab="Kasir"
    private val btPermission=100
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); db=StoreDb(this); showHome() }
    private fun text(s:String,size:Float=16f,bold:Boolean=false): TextView = TextView(this).apply { text=s; textSize=size; setTextColor(0xff20252b.toInt()); if(bold) setTypeface(null,Typeface.BOLD); setPadding(10,9,10,9) }
    private fun button(s:String, action:()->Unit): Button = Button(this).apply { text=s; setOnClickListener { action() } }
    private fun edit(hint:String, value:String=""): EditText = EditText(this).apply { this.hint=hint; setText(value); issingleLine=true; setPadding(12,4,12,4) }
    private fun start(title:String) {
        root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(14,8,14,8); setBackgroundColor(0xfff6f7f9.toInt()) }
        val scroll=ScrollView(this); scroll.addView(root)
        val shell=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xfff6f7f9.toInt()) }
        val header=TextView(this).apply { text="🛒  WARUNG KASIR\n$title"; textSize=22f; setTextColor(-1); setTypeface(null,Typeface.BOLD); setPadding(18,18,18,18); setBackgroundColor(0xff7b1e32.toInt()) }
        shell.addView(header); shell.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val nav=LinearLayout(this).apply { setBackgroundColor(0xffffffff.toInt()) }
        listOf("Kasir","Barang","Laporan").forEach { t -> nav.addView(Button(this).apply { text=t; setOnClickListener { when(t){"Kasir"->showCashier();"Barang"->showProducts();else->showReports()} } }, LinearLayout.LayoutParams(0,-2,1f)) }
        shell.addView(nav); setContentView(shell)
    }
    private fun showHome()=showCashier()
    private fun showCashier() {
        lastTab="Kasir"; start("Kasir & Transaksi")
        root.addView(text("Pilih barang untuk dimasukkan ke keranjang.",14f))
        db.products().forEach { p ->
            val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setBackgroundColor(-1) }
            val info=TextView(this).apply { text="${p.name}\n${rupiah(p.price)} • Stok ${p.stock}"; textSize=15f; setPadding(10,8,4,8) }
            row.addView(info,LinearLayout.LayoutParams(0,-2,1f))
            row.addView(button("−"){ val q=cart[p.id]?:0; if(q<=1)cart.remove(p.id) else cart[p.id]=q-1; showCashier() })
            row.addView(button("+"){ if((cart[p.id]?:0)<p.stock)cart[p.id]=(cart[p.id]?:0)+1 else toast("Stok tidak cukup"); showCashier() })
            root.addView(row); root.addView(View(this).apply{setBackgroundColor(0xffe2e5e9.toInt());layoutParams=LinearLayout.LayoutParams(-1,1)})
        }
        val all=db.products().associateBy{it.id}
        val total=cart.entries.sumOf { (id,q)->(all[id]?.price?:0)*q }
        root.addView(text("\nKERANJANG",19f,true))
        if(cart.isEmpty()) root.addView(text("Belum ada barang. Tekan + untuk memilih.",14f))
        cart.forEach { (id,q)-> all[id]?.let { root.addView(text("${it.name} × $q  =  ${rupiah(it.price*q)}",15f)) } }
        root.addView(text("TOTAL: ${rupiah(total)}",23f,true))
        val paid=edit("Uang pembayaran (angka saja)")
        root.addView(paid)
        root.addView(button("PROSES PEMBAYARAN") {
            val amount=paid.text.toString().trim().toLongOrNull()
            if(amount==null) toast("Masukkan uang pembayaran berupa angka")
            else try {
                lastReceipt=db.checkout(cart,amount); cart.clear()
                val box=TextView(this).apply { text=lastReceipt; textSize=16f; typeface=Typeface.MONOSPACE; setPadding(12,12,12,12); setBackgroundColor(-1) }
                val dlg=android.app.AlertDialog.Builder(this).setTitle("Transaksi berhasil").setView(ScrollView(this).apply{addView(box)}).setPositiveButton("Selesai",null).setNeutralButton("Cetak Bluetooth",null).create()
                dlg.setOnShowListener { dlg.getButton(-3).setOnClickListener { printBluetooth(lastReceipt) } }; dlg.show()
                showCashier()
            } catch(e:Exception) { toast(e.message ?: "Transaksi gagal") }
        })
        root.addView(button("RIWAYAT TRANSAKSI"){ showText("Riwayat",db.history()) })
    }
    private fun showProducts() {
        start("Data Barang")
        val name=edit("Nama barang"); val price=edit("Harga jual (angka)"); val cost=edit("Harga modal (angka)"); val stock=edit("Stok (angka)")
        root.addView(text("Tambah Barang",19f,true)); root.addView(name); root.addView(price); root.addView(cost); root.addView(stock)
        root.addView(button("SIMPAN BARANG") {
            val p=price.text.toString().toLongOrNull(); val c=cost.text.toString().toLongOrNull(); val st=stock.text.toString().toIntOrNull()
            if(name.text.isBlank()||p==null||c==null||st==null||p<0||c<0||st<0) toast("Lengkapi nama, harga, modal, dan stok dengan benar")
            else { db.save(name.text.toString().trim(),p,c,st); toast("Barang disimpan"); showProducts() }
        })
        root.addView(text("\nDAFTAR BARANG",19f,true))
        db.products().forEach { p ->
            val row=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(-1); setPadding(8,5,8,5) }
            row.addView(text("${p.name}\nJual ${rupiah(p.price)} • Modal ${rupiah(p.cost)} • Stok ${p.stock}",15f))
            val actions=LinearLayout(this)
            actions.addView(button("Edit"){ editProduct(p) },LinearLayout.LayoutParams(0,-2,1f))
            actions.addView(button("Hapus"){ android.app.AlertDialog.Builder(this).setMessage("Hapus ${p.name}?").setNegativeButton("Batal",null).setPositiveButton("Hapus"){_,_->db.remove(p.id);showProducts()}.show() },LinearLayout.LayoutParams(0,-2,1f))
            row.addView(actions); root.addView(row)
        }
    }
    private fun editProduct(p:Product) {
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,0,16,0)}
        val n=edit("Nama",p.name);val pr=edit("Harga jual",p.price.toString());val co=edit("Harga modal",p.cost.toString());val st=edit("Stok",p.stock.toString())
        listOf(n,pr,co,st).forEach{box.addView(it)}
        android.app.AlertDialog.Builder(this).setTitle("Edit Barang").setView(box).setNegativeButton("Batal",null).setPositiveButton("Simpan"){_,_->
            val a=pr.text.toString().toLongOrNull();val b=co.text.toString().toLongOrNull();val c=st.text.toString().toIntOrNull()
            if(n.text.isNotBlank()&&a!=null&&b!=null&&c!=null&&a>=0&&b>=0&&c>=0){db.save(n.text.toString(),a,b,c,p.id);showProducts()}else toast("Data belum benar")
        }.show()
    }
    private fun showReports() {
        start("Laporan")
        root.addView(text(db.report(),20f,true))
        root.addView(button("Lihat Riwayat"){showText("Riwayat Transaksi",db.history())})
        root.addView(button("Kembali ke Kasir"){showCashier()})
        root.addView(text("\nData tersimpan lokal di perangkat. Cadangkan data sebelum menghapus aplikasi.",13f))
    }
    private fun showText(title:String,content:String) {
        val v=TextView(this).apply{text=content;textSize=15f;typeface=Typeface.MONOSPACE;setPadding(18,18,18,18)}
        android.app.AlertDialog.Builder(this).setTitle(title).setView(ScrollView(this).apply{addView(v)}).setPositiveButton("Tutup",null).show()
    }
    private fun printBluetooth(receipt:String) {
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT),btPermission); toast("Izinkan akses Bluetooth, lalu tekan Cetak lagi"); return
        }
        try {
            val adapter=BluetoothAdapter.getDefaultAdapter() ?: throw Exception("Bluetooth tidak tersedia")
            if(!adapter.isEnabled) throw Exception("Aktifkan Bluetooth terlebih dahulu")
            val devices=adapter.bondedDevices.toList()
            if(devices.isEmpty()) throw Exception("Pasangkan printer di Pengaturan Bluetooth HP terlebih dahulu")
            val labels=devices.map { "${it.name ?: "Printer"} • ${it.address}" }.toTypedArray()
            android.app.AlertDialog.Builder(this).setTitle("Pilih printer Bluetooth").setItems(labels){_,which->
                Thread {
                    try {
                        val d=devices[which]; val socket=d.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                        adapter.cancelDiscovery(); socket.connect()
                        val out:OutputStream=socket.outputStream
                        out.write(byteArrayOf(0x1b,0x40)); out.write(receipt.toByteArray(Charsets.UTF_8)); out.write(byteArrayOf(10,10,10,0x1d,0x56,0x00)); out.flush(); socket.close()
                        runOnUiThread { toast("Perintah cetak dikirim") }
                    } catch(e:Exception) { runOnUiThread { toast("Cetak gagal: ${e.message}") } }
                }.start()
            }.setNegativeButton("Batal",null).show()
        } catch(e:Exception) { toast(e.message ?: "Bluetooth gagal") }
    }
    private fun toast(s:String) = Toast.makeText(this,s,Toast.LENGTH_LONG).show()
}
