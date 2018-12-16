package com.example.jpec.pokeguess

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import org.jsoup.Jsoup
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import android.app.Activity
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputMethodManager
import java.util.*
import java.io.*


//https://fr.wikipedia.org/wiki/Liste_des_Pok%C3%A9mon

class MainActivity : AppCompatActivity() {
    private var pokename = arrayOf("", "")
    private var number = -1
    var desc = ""
    private var imgUrl = ""
    private var uiHandler = Handler()
    private var isFrench = true

    private var rightScore = 0
    private var wrongScore = 0
    private var streakScore = 0
    private var isAlwaysRight = false
    private var hard = false


    private lateinit var mediaPlayer : MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setOnClickButtons()

        isFrench = Locale.getDefault().language == "fr"
        chooseRandomPokemon()
    }

    private fun setOnClickButtons()
    {
        validate.setOnClickListener {
            hideKeyboard(pokemon_name)
            val isTrue = when {
                pokemon_name.text.toString().equals(pokename[0], true) -> {
                    Toast.makeText(this,  String.format(resources.getString(R.string.rock), "${pokename[0].capitalize()} / ${pokename[1].capitalize()}"), Toast.LENGTH_LONG).show()
                    true
                }
                pokemon_name.text.toString().equals(pokename[1], true) -> {
                    Toast.makeText(this,  String.format(resources.getString(R.string.rock), "${pokename[0].capitalize()} / ${pokename[1].capitalize()}"), Toast.LENGTH_LONG).show()
                    true
                }
                else -> {
                    Toast.makeText(this,  String.format(resources.getString(R.string.suck), "${pokename[0].capitalize()} / ${pokename[1].capitalize()}"), Toast.LENGTH_LONG).show()
                    false
                }
            }
            pokemon_name.text.clear()
            setCurrentScore(isTrue)
            Glide.with(applicationContext).load(imgUrl).into(img)
            uiHandler.postDelayed({
                chooseRandomPokemon()
            }, 5000)
        }

        fr.setOnClickListener {
            setCurrentLocale(true)
        }

        us.setOnClickListener {
            setCurrentLocale(false)
        }

        sound.setOnClickListener {
            mediaPlayer.start()
        }

        hint.setOnClickListener {
            description.text = desc
        }

        difficulty.setOnCheckedChangeListener{ _, isChecked ->
            hard = isChecked
            if (!hard)
            {
                description.text = desc
                hint.visibility = View.GONE
            }
            else
            {
                description.text = ""
                hint.visibility = View.VISIBLE
            }
        }
    }

    private fun setCurrentScore(isTrue : Boolean)
    {
        if (isTrue)
        {
            if (isAlwaysRight)
                streakScore++
            else
                streakScore = 1
            isAlwaysRight = true
            rightScore++
        }
        else
        {
            isAlwaysRight = false
            wrongScore++
            streakScore = 0
        }
        right.text = rightScore.toString()
        wrong.text = wrongScore.toString()
        streak.text = streakScore.toString()
    }

    private fun chooseRandomPokemon()
    {
        img.setImageResource(R.drawable.question_mark)
        GlobalScope.launch()
        {
            pokename[0] = runBlocking {
                fetchRandomPokemonName()
            }
            imgUrl = runBlocking {
                fetchPokemonDescription(pokename[0])
            }
            runBlocking {
                getPokemonSound()
            }
            uiHandler.post {
                sound.performClick()
                if (!hard)
                    description.text = desc
                else
                    description.text = resources.getString(R.string.advice)
            }
        }
    }

    private fun getPokemonSound()
    {
        val url = "https://pokemoncries.com/cries/$number.mp3"
        mediaPlayer = MediaPlayer.create(this, Uri.parse(url))
    }


    private fun fetchRandomPokemonName() : String
    {
        number = Random.nextInt(1,151)
        val inputStream = if (isFrench)
            resources.openRawResource(R.raw.pokedex_fr) else resources.openRawResource(R.raw.pokedex_us)
        val pokenames = inputStream.bufferedReader().readLines()

        val inputStream2 = if (!isFrench)
            resources.openRawResource(R.raw.pokedex_fr) else resources.openRawResource(R.raw.pokedex_us)
        val pokenames2 = inputStream2.bufferedReader().readLines()
        pokename[1] = pokenames2[number - 1].substringBefore(';').toLowerCase()
        return pokenames[number - 1].substringBefore(';').toLowerCase()
    }

    private fun fetchPokemonDescription(name: String) : String
    {
        var imgUrl = ""
        val url = if (isFrench)
            "https://www.pokemon.com/fr/pokedex/$name" else "https://www.pokemon.com/us/pokedex/$name"
        try {
            val document = Jsoup.connect(url).get()
            val img = document.getElementsByClass("profile-images").first()
                .allElements.first().allElements.first().html()
            imgUrl = img.substringAfter("src=\"").substringBefore("\"")
            desc = document.getElementsByClass("pokedex-pokemon-details-right")
                .first().getElementsByClass("version-descriptions")
                .first().getElementsByClass("version-x")
                .first().allElements.first().text()
            desc = desc.replace(pokename[0], "?", ignoreCase = true)

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return imgUrl
    }

    private fun setCurrentLocale(toFrench : Boolean)
    {
        isFrench = toFrench
        val localeHelper = LocaleHelper(this)
        localeHelper.setNewLocale(this, if (toFrench) "fr" else "en")
        recreate()
    }

    override fun onDestroy () {
        super.onDestroy()
        mediaPlayer.release()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(App.localeHelper.setLocale(newBase))
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
