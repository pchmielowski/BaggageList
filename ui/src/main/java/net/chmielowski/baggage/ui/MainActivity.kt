package net.chmielowski.baggage.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.chmielowski.baggage.ui.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
