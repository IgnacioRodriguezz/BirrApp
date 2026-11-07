package com.uces.cervecerias

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.uces.cervecerias.databinding.ActivityMainBinding
import com.uces.cervecerias.ui.directory.DirectoryFragment
import com.uces.cervecerias.ui.saved.SavedFragment

/**
 * Unica Activity de la aplicacion.
 *
 * No contiene logica de datos: solo sostiene el contenedor de fragments y
 * decide cual mostrar segun la barra inferior.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        // Solo en el primer arranque: al rotar, el fragment se restaura solo.
        if (savedInstanceState == null) {
            showFragment(DirectoryFragment())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_directory -> DirectoryFragment()
                R.id.nav_saved -> SavedFragment()
                else -> return@setOnItemSelectedListener false
            }
            showFragment(fragment)
            true
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
