package android.unibo.it.sensors19
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity()  , SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerationValues: FloatArray? = null
    private var magneticValues: FloatArray? = null

    private lateinit var  outView      : TextView
    private lateinit var  connectBtn   : Button
    private lateinit var  activateBtn  : Button

    private val mqtt      =  MqttUtils()
    private var conn      =  false
    private var activated =  false
    private var msgCount  = 0

    private var azimuth = 0.0
    private var pitch   = 0.0
    private var roll    = 0.0

    private var sensorSelected = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sensorManager =  getSystemService(SENSOR_SERVICE) as SensorManager
        configureGui()
        registerSensors()
        setButtonListeners()
        //testCoap()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up activateBtn, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    protected fun configureGui(){
        setSupportActionBar(toolbar)

        val infoText = findViewById<EditText>(R.id.infoText)
        infoText.isEnabled = false

        outView           = findViewById<TextView>(R.id.textArea_information)
        activateBtn       = findViewById<Button>(R.id.activateBtn)
        activateBtn.text  = "activate"
        connectBtn        = findViewById<Button>(R.id.connBtn)
    }
/*
    fun testCoap(){
        try {
            val editText = findViewById<EditText>(R.id.editText)
            coapSupport.init("" + editText.text)
            val answer = coapSupport.readResource("robot/pos")
            addMsg(answer)
        }catch( e : Exception){
            addMsg("Please activate the CoAP resource")
        }
    }
*/
    protected fun setButtonListeners(){
        val editText = findViewById<EditText>(R.id.editText)
        editText.setText("192.168.1.6")

        val selectBtn = findViewById<RadioGroup>(R.id.select)
        connectBtn.setOnClickListener  {
            val mqttBrokerAddr = "tcp://"+editText.text+":1883" //tcp://192.168.1.6:1883
            conn = mqtt.connect(applicationContext,"android", mqttBrokerAddr)
            if( conn ) { Toast.makeText(
                this@MainActivity, "Connected to $mqttBrokerAddr", Toast.LENGTH_SHORT).show() }
            else{ Toast.makeText(
                this@MainActivity, "Problems connecting to $mqttBrokerAddr", Toast.LENGTH_SHORT).show() }
        }

        activateBtn.setOnClickListener  {
            if( ! activated ) {
                activateBtn.text = "stop"
                activated   = true
                val selectedId = selectBtn.checkedRadioButtonId
                if( selectedId > 0) {
                    val selButton = findViewById<RadioButton>(selectedId)
                    sensorSelected = selButton.text.toString()
                    Toast.makeText(
                        this@MainActivity, "Selected ${sensorSelected}", Toast.LENGTH_SHORT
                    ).show()
                }else sensorSelected = ""
            }else{
                activateBtn.text = "activate"
                activated   = false
            }
        }
    }

    private fun registerSensors(){
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL)

        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_NORMAL)

        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_NORMAL)

        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_NORMAL)

        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged( event : SensorEvent){
        when ( event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelerationValues = event.values.clone()
                if( activated && sensorSelected == "ACCELEROMETER" ) {
                    val x = event.values[0].toDouble()
                    val y = event.values[1].toDouble()
                    val z = event.values[2].toDouble()
                    emitSensorValues(x, y, z, "acceleromenter")
                    outMsg("x=$x y=$y z=$z")
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                if( activated && sensorSelected == "GYROSCOPE" ) {
                    val gyroValues = event.values.clone()
                    val x = gyroValues[0].toDouble()
                    val y = gyroValues[1].toDouble()
                    val z = gyroValues[2].toDouble()
                    outMsg("x=$x y=$y z=$z")
                    emitSensorValues(x, y, z, "gyroscope")
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magneticValues = event.values.clone()
                val rotationMatrix = generateRotationMatrix()
                if (rotationMatrix != null && activated && sensorSelected == "ACCELEROMETERMAGNETIC_FIELD"){
                    determineOrientation("orientation",rotationMatrix,true)
                }
                if( activated && sensorSelected == "MAGNETIC_FIELD" ) {
                    val x = event.values[0].toDouble()
                    val y = event.values[1].toDouble()
                    val z = event.values[2].toDouble()
                    outMsg("x=$x y=$y z=$z")
                    emitSensorValues(x, y, z, "magneticfield")
                }
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                if (activated && sensorSelected == "ROTATION_VECTOR") {
                    val rotMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotMatrix, event.values.clone() )
                    determineOrientation("rotVector",rotMatrix,true)
                }
            }
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                if (activated && sensorSelected == "GAME_ROTATION_VECTOR" ) {
                    val rotMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector( rotMatrix, event.values.clone() )
                    if ( activated) {
                        determineOrientation("gamerotation",rotMatrix,true)
                    }
                }//activated
            }
        }
    }

    override fun onAccuracyChanged(sensor : Sensor, accuracy : Int ){}

    /**
     * Generates a rotation matrix using the member data stored in
     * accelerationValues and magneticValues.
     *
     * @return The rotation matrix returned from
     * {@link android.hardware.SensorManager#getRotationMatrix(float[], float[], float[], float[])}
     * or <code>null</code> if either <code>accelerationValues</code> or
     * <code>magneticValues</code> is null.
     */

    private fun generateRotationMatrix(): FloatArray? {
        var rotationMatrix: FloatArray? = null
        if (accelerationValues != null && magneticValues != null) {
            rotationMatrix = FloatArray(16)
            val rotationMatrixGenerated: Boolean

            rotationMatrixGenerated = SensorManager.getRotationMatrix(
                rotationMatrix, null, accelerationValues, magneticValues
            )
            if (!rotationMatrixGenerated){
                rotationMatrix = null
                Toast.makeText(
                    this@MainActivity, "WARNING: rotationMatrix is null", Toast.LENGTH_SHORT
                ).show()
            }
        }
        return rotationMatrix
    }

    private fun emitSensorValues(x : Double, y : Double, z : Double, qakev : String  ){
        val msgout = "msg( androidSensor, event, android, none, androidSensor($qakev, $x, $y, $z), ${msgCount++} )"
        mqtt.sendMsg(msgout, "unibo/qak/events")    //see receiver.qak in it.unibo.python
    }

    private fun determineOrientation( qakev: String, rotationMatrix : FloatArray, show : Boolean = false) {
        val orientValues = FloatArray(3)
        SensorManager.remapCoordinateSystem(
            rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, rotationMatrix
        )
        SensorManager.getOrientation(rotationMatrix, orientValues)
        azimuth = Math.toDegrees(orientValues[0].toDouble())
        pitch   = Math.toDegrees(orientValues[1].toDouble())
        roll    = Math.toDegrees(orientValues[2].toDouble())
        emitSensorValues(azimuth, pitch, roll, qakev )
        if( show ) outMsg("a=$azimuth p=$pitch r=$roll")
    }

    protected fun outMsg( msg : String ){
        outView.setText( msg, TextView.BufferType.NORMAL )
    }
    protected fun addMsg( msg : String ){
        outView.append( "$msg\n"  )
    }

}