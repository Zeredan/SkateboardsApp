package com.example.skateboardapp

import android.content.Context
import android.os.Bundle
import android.util.SparseArray
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.Color.Companion.Yellow
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.concurrent.thread

class ColorInfo
{
    companion object {
        var currentAngle: MutableState<Float> = mutableStateOf(0f)
    }
}

@Composable fun rainbowColored(frequency: Float) : Color
{
    return Color.hsl((ColorInfo.currentAngle!!.value * frequency % 360), 1f, 0.5f)
}

fun Modifier.twoColoredTemperatureBg(sideColor: Color, midColor: Color) : Modifier
{
    return this.background(object : ShaderBrush()
    {
        override fun createShader(size: Size): Shader {
            return LinearGradientShader(Offset(0f, 0f), Offset(size.width, 0f), listOf(sideColor, midColor, sideColor))
        }

    })
}

data class Container<T>(var value: T)
fun<T> T.toContainer() = Container(this)


@Composable
fun LinearGradientalBox(modifier: Modifier, alpha: Float, colors: List<Color>, content: @Composable () -> Unit)
{
    return Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.then(
            Modifier
                .background(object : ShaderBrush(){
                    @Override
                    override fun createShader(size: Size): Shader
                    {
                        return LinearGradientShader(Offset(0f, 0f), Offset(size.width, size.height * Math.tan(alpha / 180 * Math.PI).toFloat()), colors)
                    }
                })
        )
    )
    {
        content()
    }
}

@Composable
fun GradientalBox(modifier: Modifier, mainColor: Color = Color.Yellow, content: @Composable () -> Unit)
{
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.then(
            Modifier
                .clip(CutCornerShape(10f))
                .background(
                    object : ShaderBrush() {
                        override fun createShader(size: Size): Shader {
                            return RadialGradientShader(
                                Offset(size.width / 2, size.height / 2),
                                size.width / 3,
                                listOf(White, mainColor, White, White, mainColor, Black),
                                listOf(0f, 0.3f, 0.4f, 0.45f, 0.95f, 1f)
                            )
                        }
                    }
                )
        )
    )
    {
        content()
    }
}

@Composable
fun VerticalGrid(modifier: Modifier, elements: List<@Composable () -> Unit>, verticalArrangement: Arrangement.Vertical, horizontalAlignment: Alignment.Horizontal, maxWideElements: Int)
{
    Column(
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,

        modifier = modifier
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
    )
    {
        elements.size.run{this / maxWideElements to this % maxWideElements}.run nRowsTonLastRow@{
            repeat(this.first){ rowInd ->
                Row(
                )
                {
                    repeat(maxWideElements){ elemInd ->
                        elements[rowInd * maxWideElements + elemInd]()
                        if (elemInd < maxWideElements - 1) Spacer(modifier = Modifier.width(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            if (this.second > 0)
            {
                Row(
                )
                {
                    repeat(this@nRowsTonLastRow.second) { elemInd ->
                        elements[this@nRowsTonLastRow.first * maxWideElements + elemInd]()
                        if (elemInd < this@nRowsTonLastRow.second - 1) Spacer(modifier = Modifier.width(20.dp))
                    }
                }
            }
        }
    }
}


enum class Wheel(val Image : Int, val rigidity: String, val diameter: Float){
    RigidWheel(R.drawable.wheel, "Rigid", 10f),
    SoftWheel(R.drawable.wheel, "Soft", 10f),
    LongBoardWheel(R.drawable.wheel, "Very soft", 10f),
    LegoWheel(R.drawable.wheel, "Specific", 10f),
    WideWheel(R.drawable.wheel, "Soft", 10f)
}

enum class Suspension(val Image : Int, description: String){
    ClassicSuspension(R.drawable.suspension, "classic"),
    LongBoardSuspension(R.drawable.suspension, "big one"),
    SurfSuspension(R.drawable.suspension, "unreal mobility")
}

enum class Deck(val Image : Int, val length: Int, val width: Int){
    CruiserDeck(R.drawable.deck, 80, 40),
    ClassicDeck(R.drawable.deck, 100, 50),
    LongBoardDeck(R.drawable.deck, 200, 55),
    SurfDeck(R.drawable.deck, 150, 60)
}

enum class Bearing(val Image : Int, description: String){
    Bearing1(R.drawable.bearing, "Awfull"),
    Bearing3(R.drawable.bearing, "Bad"),
    Bearing5(R.drawable.bearing, "Cheap"),
    Bearing7(R.drawable.bearing, "Good"),
    Bearing9(R.drawable.bearing, "Perfect one")
}

enum class Language {
    RU(),
    ENG()
}

enum class MenuContentName(){
    Skateboards()
    {
        override fun getName(language: Language): String
        {
            return when(language)
            {
                Language.RU -> "Скейтборды"
                Language.ENG -> "Skateboards"
                else -> ""
            }
        }
    },
    Armour()
    {
        override fun getName(language: Language): String
        {
            return when(language)
            {
                Language.RU -> "Броня"
                Language.ENG -> "Armour"
                else -> ""
            }
        }
    },
    Selected_stuff()
    {
        override fun getName(language: Language): String
        {
            return when(language)
            {
                Language.RU -> "Снаряжение"
                Language.ENG -> "Selected stuff"
                else -> ""
            }
        }
    },
    RoadMap()
    {
        override fun getName(language: Language): String
        {
            return when(language)
            {
                Language.RU -> "Путь развития"
                Language.ENG -> "RoadMap"
                else -> ""
            }
        }
    },
    Options()
    {
        override fun getName(language: Language): String
        {
            return when(language)
            {
                Language.RU -> "Настройки"
                Language.ENG -> "Options"
                else -> ""
            }
        }
    };

    abstract fun getName(language: Language) : String
}


data class SkateBoardCharacteristics(var speed: Float, var turnRate: Float, var acceleration: Float, var dessepative: Float, var buffs: MutableList<String>)
abstract class SkateboardElements()
{
    @Composable
    abstract fun getCharacteristics() : SkateBoardCharacteristics
    @Composable
    protected open fun ItemIcon(image: Int, text: String, size: Dp, onClick : () -> Unit)
    {
        Column(
            modifier = Modifier
                .width(size)
                .padding(4.dp)
                .border(1.dp, Color.Yellow),
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            Image(painter = painterResource(id = image), contentDescription = null)
            Text(modifier = Modifier.scale(1f), text = text, textAlign = TextAlign.Center)
        }
    }
    @Composable
    public abstract fun LeftColumn(modifier: Modifier)
    @Composable
    public abstract fun RightColumn(modifier: Modifier)
}

class StandardSkateboardElements(val wheels : Wheel, val bearing: Bearing, val suspensionF: Suspension, val suspensionB: Suspension, val deck : Deck) : SkateboardElements()
{
    @Composable
    override fun getCharacteristics(): SkateBoardCharacteristics {
        return SkateBoardCharacteristics(
            speed = when(wheels)
            {
                Wheel.LongBoardWheel -> 1.6f
                Wheel.RigidWheel -> 1f
                Wheel.SoftWheel -> 1.3f
                Wheel.WideWheel -> 1.2f
                Wheel.LegoWheel -> 1.1f
            } *
            when(bearing)
            {
                Bearing.Bearing1 -> 1f
                Bearing.Bearing3 -> 1.2f
                Bearing.Bearing5 -> 1.4f
                Bearing.Bearing7 -> 1.6f
                Bearing.Bearing9 -> 1.65f
            } * 12f,
            turnRate = when(deck)
            {
                Deck.ClassicDeck -> 1.5f
                Deck.CruiserDeck -> 1.4f
                Deck.LongBoardDeck -> 1f
                Deck.SurfDeck -> 1.2f
            } * when(suspensionF)
            {
                Suspension.SurfSuspension -> 7f
                else -> 1f
            },
            acceleration = when(deck)
            {
                Deck.LongBoardDeck -> 1f
                Deck.SurfDeck -> 1f
                Deck.ClassicDeck -> 1.5f
                Deck.CruiserDeck -> 1.6f
            },
            dessepative = when(wheels)
            {
                Wheel.RigidWheel -> 2f
                Wheel.WideWheel -> 1.2f
                Wheel.LegoWheel -> 1.4f
                Wheel.SoftWheel -> 1.1f
                Wheel.LongBoardWheel -> 1f
            },
            buffs = mutableListOf<String>().apply{
                if (deck == Deck.ClassicDeck) this += "Исполнение прыжковых трюков"
                if (suspensionF == Suspension.SurfSuspension) this.addAll(arrayOf("Разгон без толчков", "Невероятные развороты"))
                if ((deck == Deck.LongBoardDeck) && (suspensionF == Suspension.LongBoardSuspension) && (suspensionB == Suspension.LongBoardSuspension)) this += "Спуск на высокой скорости"
            }
        )
    }
    @Composable
    override fun LeftColumn(modifier: Modifier)
    {
        Column(
            modifier = Modifier
                .then(modifier)
                .height(250.dp)
                .clip(CutCornerShape(10.dp))
                .background(object : ShaderBrush() {
                    override fun createShader(size: Size): Shader {
                        return LinearGradientShader(
                            Offset(0f, 0f),
                            Offset(size.width, size.height),
                            listOf(Yellow, Red)
                        )
                    }
                })
                .verticalScroll(rememberScrollState())
                .padding(5.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            ItemIcon(
                wheels.Image,
                "колеса",
                60.dp,
                {

                }
            )
            Spacer(modifier = modifier.height(10.dp))
            ItemIcon(
                bearing.Image,
                "подшипники",
                60.dp,
                {

                }
            )
        }
    }
    @Composable
    override fun RightColumn(modifier: Modifier)
    {
        Column(
            modifier = Modifier
                .then(modifier)
                .height(250.dp)
                .clip(CutCornerShape(10.dp))
                .background(object : ShaderBrush() {
                    override fun createShader(size: Size): Shader {
                        return LinearGradientShader(
                            Offset(0f, 0f),
                            Offset(size.width, size.height),
                            listOf(Yellow, Red)
                        )
                    }
                })
                .verticalScroll(rememberScrollState())
                .padding(5.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            ItemIcon(
                deck.Image,
                "доска",
                60.dp,
                {

                }
            )
            Spacer(modifier = modifier.height(10.dp))
            ItemIcon(
                suspensionF.Image,
                "подвеска",
                60.dp,
                {

                }
            )
            Spacer(modifier = modifier.height(10.dp))
            ItemIcon(
                suspensionB.Image,
                "подвеска",
                60.dp,
                {

                }
            )
        }
    }
}
class SkateBoard(val Name: String, val Description: String, var image: Int, val Difficulty: String, val Rarity: String, val Elements: SkateboardElements)
{

}

data class Quest(var text: String, var Image: Int, var videoUrl: String)

data class ContentInformation(var content: @Composable () -> Unit, var resourceImage: Int, var clue: @Composable () -> Unit)
class Data {
    companion object {
        var ourSelectedSkateboard: MutableState<SkateBoard?> = mutableStateOf(null)
        var context: Context? = null
        var skateboards: MutableList<SkateBoard> = mutableListOf()

        var roadMap: MutableMap<String, MutableList<Quest>> = mutableMapOf(
            "Круизер" to mutableListOf(
                Quest("Ходьба на 1 ноге со скейтбордом", R.drawable.walk1leg, ""),
                Quest("Вставание на скейтборд", R.drawable.standup, ""),
                Quest("Езда на скейтборде без ускорения", R.drawable.move, ""),
                Quest("Езда с ускорением", R.drawable.surge, ""),
                Quest("Повороты. Маневренность", R.drawable.manevours, ""),
                Quest("Быстрый старт езды", R.drawable.downskate, ""),
                Quest("Быстрый финиш езды", R.drawable.upskate, ""),
                Quest("БЕСКОНЕЧНАЯ ПРАКТИКА", R.drawable.infinity, "")
            ),
            "Трюковый Скейтборд" to mutableListOf(
                Quest("Ходьба на 1 ноге со скейтбордом", R.drawable.walk1leg, ""),
                Quest("Вставание на скейтборд", R.drawable.standup, ""),
                Quest("Езда на скейтборде без ускорения", R.drawable.move, ""),
                Quest("Езда с ускорением", R.drawable.surge, ""),
                Quest("Повороты. Маневренность", R.drawable.manevours, ""),
                Quest("Быстрый старт езды", R.drawable.downskate, ""),
                Quest("Быстрый финиш езды", R.drawable.upskate, ""),
                Quest("Олли", R.drawable.ollie, ""),
                Quest("Кикфлип", R.drawable.kickflip, ""),
                Quest("Гринд", R.drawable.grind, ""),
                Quest("БЕСКОНЕЧНАЯ ПРАКТИКА", R.drawable.infinity, "")
            ),
            "Лонгборд" to mutableListOf(
                Quest("Ходьба на 1 ноге со скейтбордом", R.drawable.walk1leg, ""),
                Quest("Вставание на скейтборд", R.drawable.standup, ""),
                Quest("Езда на скейтборде без ускорения", R.drawable.move, ""),
                Quest("Езда с ускорением", R.drawable.surge, ""),
                Quest("Повороты. Маневренность", R.drawable.manevours, ""),
                Quest("Быстрый старт езды", R.drawable.downskate, ""),
                Quest("Быстрый финиш езды", R.drawable.upskate, ""),
                Quest("Танцевальные элементы", R.drawable.dancing, ""),
                Quest("БЕСКОНЕЧНАЯ ПРАКТИКА", R.drawable.infinity, "")
            ),
            "Серфскейт" to mutableListOf(
                Quest("Ходьба на 1 ноге со скейтбордом", R.drawable.walk1leg, ""),
                Quest("Вставание на скейтборд", R.drawable.standup, ""),
                Quest("Езда на скейтборде без ускорения", R.drawable.move, ""),
                Quest("Езда с ускорением", R.drawable.surge, ""),
                Quest("Повороты. Маневренность", R.drawable.manevours, ""),
                Quest("Быстрый старт езды", R.drawable.downskate, ""),
                Quest("Быстрый финиш езды", R.drawable.upskate, ""),
                Quest("Разворот на большой угол", R.drawable.supermanevours, ""),
                Quest("Сверхманевренность", R.drawable.megaspeed, ""),
                Quest("БЕСКОНЕЧНАЯ ПРАКТИКА", R.drawable.infinity, "")
            )
        )

        var MenuContents: MutableMap<MenuContentName, ContentInformation> = mutableMapOf(
            MenuContentName.Skateboards to ContentInformation(
                { SkateBoards() },
                R.drawable.skateboards,
                { Text("Информация о видах скейтбордов и их сравнение") }
            ),
            MenuContentName.Armour to ContentInformation(
                { Armor() },
                R.drawable.crimson,
                { Text("Информация о видах брони") }
            ),
            MenuContentName.Selected_stuff to ContentInformation(
                { SelectedBuild() },
                R.drawable.build,
                { Text("Выбранный вами скейтборд + броня, здесь изображены ваши характеристики") }
            ),
            MenuContentName.RoadMap to ContentInformation(
                { RoadMap() },
                R.drawable.xmark,
                { Text("Ваш путь развития в скейтбординге") }
            ),
            MenuContentName.Options to ContentInformation(
                { Options() },
                R.drawable.rearm,
                { Text("Тут можно настроить внешний вид и данные приложения") }
            )
        )
    }
}

@Composable fun SkateboardCard(modifier: Modifier, selectedSkateBoard: SkateBoard)
{
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            modifier = Modifier.scale(2f),
            text = selectedSkateBoard.Name,
            color = when (selectedSkateBoard.Rarity) {
                "Common" -> Color.White
                "Rare" -> Color.hsl(30f, 1f, 0.5f)
                "Epic" -> Color.Magenta
                "Legendary" -> Color.hsl(
                    (ColorInfo.currentAngle!!.value % 360),
                    1f,
                    0.5f

                )

                else -> Color.White
            }
        )
        Spacer(modifier = Modifier.height(6.dp))
        Divider(thickness = 2.dp, color = Color.Black)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        )
        {
            selectedSkateBoard.Elements?.LeftColumn(Modifier)
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.width(170.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            )
            {
                Image(
                    modifier = Modifier.size(170.dp, 170.dp),
                    painter = painterResource(selectedSkateBoard.image),
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            selectedSkateBoard.Elements.RightColumn(Modifier)
        }
        selectedSkateBoard.Elements.getCharacteristics().run {
            Text(
                "скорость: ${this.speed}\nускорение: ${this.acceleration}\nпотеря наката: ${this.dessepative}\nскорость поворота: ${this.turnRate}",
                color = Color.Cyan
            )
            Text(
                "".toContainer().apply{this@run.buffs.forEach{buff -> this.value += "♦ ${buff}\n"}}.value,
                color = Color.Green
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        )
        {
            Text(selectedSkateBoard.Description, color = Color.White)
        }
    }
}

@Composable fun SkateboardSelector(modifier: Modifier, onSelect: (SkateBoard) -> Unit)
{
    VerticalGrid(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        elements = Data.skateboards.map{ skate ->
            @Composable {
                Column(
                    modifier = Modifier
                        .clickable {
                            onSelect(skate)
                        }
                        .padding(10.dp)
                        .size(150.dp, 150.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                )
                {
                    Image(
                        modifier = Modifier
                            .size(120.dp, 120.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        painter = painterResource(id = skate.image),
                        contentDescription = null
                    )
                    Text(skate.Name)
                }
            }
        },
        maxWideElements = 2
    )
}

@OptIn(ExperimentalStdlibApi::class)
@Composable fun SkateBoards()
{
    var selectedSkateBoard = remember{ mutableStateOf<SkateBoard?>(null) }
    var selectedElement = remember{ mutableStateOf<(@Composable () -> Unit)?>(null) }
    if (selectedElement.value != null)
    {
        AlertDialog(
            onDismissRequest = { /*TODO*/ },
            dismissButton = {},
            confirmButton = {},
            text =
            {
                Text("alpaca")
            }
        )
    }
    selectedSkateBoard.value
        ?.let {
            Column(
                modifier = Modifier.fillMaxSize()
            )
            {
                SkateboardCard(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    selectedSkateBoard = selectedSkateBoard.value!!
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clickable {
                            selectedSkateBoard.value = null
                        }
                        .twoColoredTemperatureBg(Color.Cyan, Color.Blue)
                        .fillMaxWidth()
                        .height(50.dp),
                )
                {
                    Text(
                        modifier = Modifier.scale(2f),
                        text = "НАЗАД",
                        color = Color.LightGray
                    )
                }
                BackHandler(true) {
                    selectedSkateBoard.value = null
                }
            }
        }
        ?: 0.let{
            SkateboardSelector(
                modifier = Modifier.fillMaxSize(),
                onSelect = {sk -> selectedSkateBoard.value = sk}
            )
        }
}

@Composable fun Armor()
{
    Text("Броня - то, что поможет вам получать меньше урона при падениях, однако замедлит ваши перемещения.\nТак же стоит дополнительных денег и ухудшает внешний вид")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SelectedBuild()
{
    var selectedPendingSkate = remember{ mutableStateOf<SkateBoard?>(null) }
    var requireSelection = remember{ mutableStateOf(false) }

    LaunchedEffect(Unit)
    {
        //File(Data.context!!.filesDir, "selectedSkateboard.data").let{if (it.isFile) selectedSkateboard.value = (try{it.readLines().run{Data.skateboards.firstOrNull{sk -> sk.Name.also{println(it)} == this[0]}}} catch(e: Exception){throw e}) }
    }
    //selectedSkateboard.value = try{Data.skateboards[2]} catch(e: Exception){null}
    //File(Data.context!!.filesDir, "selectedSkateboard.data").let{if (it.isFile) selectedSkateboard.component2()(try{it.readLines().run{Data.skateboards.firstOrNull{sk -> sk.Name.also{println(it)} == this[0]}}} catch(e: Exception){throw e}) }

    if (!requireSelection.value) {
        Data.ourSelectedSkateboard.value
            ?.let {
                selectedPendingSkate.value
                    ?.let{
                        AlertDialog(
                            onDismissRequest = { selectedPendingSkate.value = null; requireSelection.value = true },
                        )
                        {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                            {
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            Data.ourSelectedSkateboard.value =
                                                selectedPendingSkate.value
                                            selectedPendingSkate.value = null
                                            if (Data.ourSelectedSkateboard.value != null) File(
                                                Data.context!!.filesDir,
                                                "selectedSkateboard.data"
                                            ).run {
                                                this.createNewFile(); this
                                                .writer()
                                                .let {
                                                    it.write(
                                                        Data.ourSelectedSkateboard.value?.Name ?: ""
                                                    ); it.close()
                                                }
                                            }
                                        }
                                        .height(100.dp)
                                        .weight(1f)
                                        .twoColoredTemperatureBg(Color.Green, Color.White),
                                    contentAlignment = Alignment.Center
                                )
                                {
                                    Text(modifier = Modifier
                                        .scale(1.5f)
                                        .padding(4.dp), text = "ЗАМЕНИТЬ")
                                }
                                Spacer(modifier = Modifier.width(40.dp))
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            selectedPendingSkate.value = null
                                            requireSelection.value = true
                                        }
                                        .height(100.dp)
                                        .weight(1f)
                                        .twoColoredTemperatureBg(Color.Red, Color.White),
                                    contentAlignment = Alignment.Center
                                )
                                {
                                    Text(modifier = Modifier
                                        .scale(1.5f)
                                        .padding(4.dp), text = "Отмена")
                                }
                            }
                        }
                    }
                    ?: 0.let{
                        Column(
                            modifier = Modifier.fillMaxSize()
                        )
                        {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                                    .twoColoredTemperatureBg(Color.Black, Color.White),
                                contentAlignment = Alignment.Center
                            )
                            {
                                Text(modifier = Modifier.scale(2f), text = "ВАШ СКЕЙТБОРД")
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            SkateboardCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                selectedSkateBoard = Data.ourSelectedSkateboard.value!!
                            )
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        requireSelection.value = true
                                    }
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .twoColoredTemperatureBg(Color.Black, Color.White),
                                contentAlignment = Alignment.Center
                            )
                            {
                                Text(modifier = Modifier.scale(2f), text = "Выбрать другой скейтборд")
                            }
                        }
                    }
            }
            ?: 0.let {
                LaunchedEffect(
                    selectedPendingSkate.value
                )
                {
                    Data.ourSelectedSkateboard.value = selectedPendingSkate.value
                    selectedPendingSkate.value = null
                    if (Data.ourSelectedSkateboard.value != null) File(Data.context!!.filesDir, "selectedSkateboard.data").run{this.createNewFile(); this.writer().let{ it.write(Data.ourSelectedSkateboard.value?.Name ?: ""); it.close()}}
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                )
                {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        modifier = Modifier.scale(2f),
                        text = "СКЕЙТБОРД НЕ ВЫБРАН",
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clickable {
                                requireSelection.value = true
                            }
                            .fillMaxWidth()
                            .height(50.dp)
                            .twoColoredTemperatureBg(Color.Black, Color.White),
                        contentAlignment = Alignment.Center
                    )
                    {
                        Text(modifier = Modifier.scale(2f), text = "Выбрать скейтборд")
                    }
                }
            }
    }
    else
    {
        Column(
            modifier = Modifier.fillMaxSize()
        )
        {
            SkateboardSelector(
                modifier = Modifier.weight(1f),
                onSelect = { sk ->
                    selectedPendingSkate.value = sk
                    requireSelection.value = false
                }
            )
            Box(
                modifier = Modifier
                    .clickable {
                        requireSelection.value = false
                    }
                    .fillMaxWidth()
                    .height(30.dp)
                    .twoColoredTemperatureBg(Color.White, Color.Black),
                contentAlignment = Alignment.Center
            )
            {
                Text(modifier = Modifier.scale(2f), text = "Отмена")
            }
            BackHandler(true) {
                requireSelection.value = false
            }
        }
    }
}


@Composable fun RoadMap()
{
    var selectedQuest = remember{ mutableStateOf<Quest?>(null) }

    selectedQuest.value
        ?.let {
            Column(

            )
            {
                val context = LocalContext.current
                val player = ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"))
                    /*setMediaItem(MediaItem.fromUri(
                        "https://youtu.be/40mcfWwV9Us".run link@
                        {
                            var downloadUrl = ""
                            object : YouTubeExtractor(context)
                            {
                                override fun onExtractionComplete(
                                    ytFiles: SparseArray<YtFile>?,
                                    videoMeta: VideoMeta?
                                )
                                {
                                    if (ytFiles != null) {
                                        val itag = 22
                                        downloadUrl = ytFiles[itag].url
                                        println(downloadUrl)
                                    }
                                }

                            }.extract(this, true, true)
                            downloadUrl.also{s -> repeat(20){println(s)} }
                        })
                    )*/
                }
                val playerView = PlayerView(context)

                playerView.player = player

                LaunchedEffect(player) {
                    player.prepare()
                    player.playWhenReady = true
                }

                AndroidView(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    factory = {
                        playerView
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable {
                            selectedQuest.value = null
                        }
                        .twoColoredTemperatureBg(Color.Black, Color.White),
                    contentAlignment = Alignment.Center
                )
                {
                    Text(modifier = Modifier.scale(2f), text = "Назад")
                }
                BackHandler(true) {
                    selectedQuest.value = null
                }
            }

        }
        ?: 0.let{
            Column(
                modifier = Modifier
                    .twoColoredTemperatureBg(Color.Magenta, Color.hsl(330f, 1f, 0.5f))
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            )
            {
                Data.skateboards.forEach { skateboard ->
                    Data.roadMap[skateboard.Name]?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(20.dp)
                                .alpha(if (Data.ourSelectedSkateboard.value == skateboard) 1f else 0.3f),
                        )
                        {
                            Data.roadMap[skateboard.Name]!!.forEach { quest ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .run {
                                            if (Data.ourSelectedSkateboard.value == skateboard)
                                                this.clickable {
                                                    selectedQuest.value = quest
                                                }
                                            else this
                                        }
                                )
                                {
                                    Image(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        painter = painterResource(id = quest.Image),
                                        contentDescription = null
                                    )
                                    Text(quest.text, color = Color.Blue)
                                }
                                Spacer(modifier = Modifier.width(20.dp))
                            }
                        }
                        Divider(thickness = 3.dp, color = Color.Cyan)
                    }
                }
            }
        }
}

@Composable fun Options()
{
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
    {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.Center
        )
        {
            Text(text = "Настройки", color = Color.White)
        }
        Divider(thickness = 2.dp, color = Color.White)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable {
                    File(Data.context!!.filesDir, "FirstTimeMarker.marker").delete()
                },
            verticalAlignment = Alignment.CenterVertically
        )
        {
            Spacer(modifier = Modifier.width(10.dp))
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = R.drawable.numberone),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text("Удалить данные о первом входе", color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = R.drawable.fire),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable {
                    File(Data.context!!.filesDir, "selectedSkateboard.data").delete()
                    Data.ourSelectedSkateboard.value = null
                },
            verticalAlignment = Alignment.CenterVertically
        )
        {
            Spacer(modifier = Modifier.width(10.dp))
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = R.drawable.fadedskate),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text("Убрать выбранный скейтборд", color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = R.drawable.fire),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
    }
}

@Composable fun StartingMenu(setBShowStartmenu: (Boolean) -> Unit, qwer: MutableState<Boolean?>)
{
    AlertDialog(
        modifier = Modifier
            .fillMaxSize(0.8f)
            .background(object : ShaderBrush() {
                override fun createShader(size: Size): Shader {
                    return LinearGradientShader(
                        Offset(0f, size.height),
                        Offset(size.width, 0f),
                        listOf(Color.Magenta, Color.Blue)
                    )
                }

            }),
        onDismissRequest = { setBShowStartmenu(false); qwer.value = false },
        dismissButton = {
            GradientalBox(
                modifier = Modifier
                    .size(150.dp, 70.dp)
                    .pointerInput(Unit)
                    {
                        this.detectTapGestures {
                            setBShowStartmenu(false)
                        }
                    }
            )
            {
                Text("Закрыть")
            }
        },
        confirmButton = {
        },
        title = {
            Text(
                modifier = Modifier.scale(1f),
                text = "Добро пожаловать в \"Путь развития скейтбордиста\"",
                color = Color.Magenta
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                Divider(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                )
                Text("В этом приложении есть много полезной информации про скейтборды,\nа так же обучающие гайды и возможности\nсравнивать между собой доски,\nсмотреть последовательно прогресс обучения и прочее", color = Color.Black)
                Divider(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                )
                Text("Для перемещения по приложению используйте НИЖНЮЮ ПАНЕЛЬ", color = Color.Green)
            }
        }
    )
}


class MainActivity : ComponentActivity()
{
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContent {
            //NavHost()
            Data.context = LocalContext.current
            LaunchedEffect(Unit)
            {
                Data.skateboards = mutableListOf(
                    SkateBoard(
                        "Круизер",
                        Data.context?.getString(R.string.cruiser_description) ?: "",
                        R.drawable.cruiser,
                        "Easy",
                        "Rare",
                        StandardSkateboardElements(
                            Wheel.SoftWheel,
                            Bearing.Bearing7,
                            Suspension.ClassicSuspension,
                            Suspension.ClassicSuspension,
                            Deck.CruiserDeck
                        )
                    ),
                    SkateBoard(
                        "Трюковый Скейтборд",
                        Data.context?.getString(R.string.classic_description) ?: "",
                        R.drawable.classic,
                        "Hard",
                        "Common",
                        StandardSkateboardElements(Wheel.RigidWheel, Bearing.Bearing7, Suspension.ClassicSuspension, Suspension.ClassicSuspension, Deck.ClassicDeck)
                    ), SkateBoard(
                        "Лонгборд",
                        Data.context?.getString(R.string.longboard_description) ?: "",
                        R.drawable.longboard,
                        "Medium",
                        "Epic",
                        StandardSkateboardElements(Wheel.LongBoardWheel, Bearing.Bearing9, Suspension.LongBoardSuspension, Suspension.LongBoardSuspension, Deck.LongBoardDeck)
                    ),
                    SkateBoard(
                        "Серфскейт",
                        Data.context?.getString(R.string.surfskate_description) ?: "",
                        R.drawable.surfskate,
                        "Extremly hard",
                        "Legendary",
                        StandardSkateboardElements(Wheel.WideWheel, Bearing.Bearing9, Suspension.SurfSuspension, Suspension.LongBoardSuspension, Deck.SurfDeck)
                    )
                )

                Data.ourSelectedSkateboard.value = try{
                    File(Data.context!!.filesDir, "selectedSkateboard.data").run{
                        this.readLines()[0].run{
                            Data.skateboards.first{sk -> sk.Name == this}
                        }
                    }
                }
                catch(e: Exception){
                    null
                }
            }

            LaunchedEffect(Unit)
            {
                thread {
                    runBlocking {
                        while (true) {
                            if (ColorInfo.currentAngle != null) {
                                ColorInfo.currentAngle!!.value =
                                    if (ColorInfo.currentAngle!!.value >= 359) ColorInfo.currentAngle!!.value + 1 else ColorInfo.currentAngle!!.value + 1
                            }
                            delay(10)
                        }
                    }
                }
            }

            var selectedMenuContent = remember{ mutableStateOf<MenuContentName?>(null) }
            var bShowStartMenu = remember{ mutableStateOf<Boolean?>(null) }
            LaunchedEffect(key1 = Unit) {
                File(Data.context!!.filesDir, "FirstTimeMarker.marker").run file@{
                    this.exists().takeIf { it }
                        ?.let {
                            bShowStartMenu.value = false
                        }
                        ?: let {
                            this.createNewFile()
                            bShowStartMenu.value = true
                        }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
            )
            {
                bShowStartMenu.value?.takeIf{ it }?.let{
                    StartingMenu(bShowStartMenu.component2(), bShowStartMenu)
                }
                LinearGradientalBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                        //.fillMaxHeight(),
                    alpha = 30f,
                    colors = listOf(Color.LightGray, Color.DarkGray)
                )
                {
                    Data.MenuContents[selectedMenuContent.value]?.content?.invoke()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color.Black)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.Top,

                    //contentPadding = PaddingValues(20.dp)
                )
                {
                    Data.MenuContents.entries.forEachIndexed { ind, entry ->
                        var bShowClue = remember{ mutableStateOf(false) }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(110.dp, 110.dp)
                                .combinedClickable(
                                    onClick =
                                    {
                                        selectedMenuContent.value = entry.key
                                    },
                                    onLongClick =
                                    {
                                        bShowClue.value = true
                                    }
                                )
                        )
                        {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            )
                            {
                                Image(
                                    modifier = Modifier
                                        .size(75.dp, 75.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    painter = painterResource(id = entry.value.resourceImage),
                                    contentDescription = null
                                )
                                Text(entry.key.getName(Language.RU), color = if (selectedMenuContent.value == entry.key) Color.Green else Color.Yellow)
                            }
                        }
                        //if (ind < MenuContents.size - 1) Spacer(modifier = Modifier.width(20.dp))
                        DropdownMenu(
                            expanded = bShowClue.value,
                            onDismissRequest = {
                                bShowClue.value = false
                            },
                        )
                        {
                            entry.value.clue()
                            DropdownMenuItem(text = { Text("OK") }, onClick = { bShowClue.value = false })
                        }
                    }
                }
            }
        }
    }
}
