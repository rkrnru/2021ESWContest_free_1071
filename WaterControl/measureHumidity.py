import RPi.GPIO as GPIO
import time
import spidev

class MeasureHumidity():
    def __init__(self):
        self.humidity = 0
        
        #수분 센서의 GPIO 번호
        self.DIGIT = 23

        #Humidity sensor output value when sensor is soaked in water.
        self.HUM_MAX = 1023

        #Initial setting of motor drive
        #기본 핀 설정이랑 다를 때, 경고 보내지 말라고 설정
        GPIO.setwarnings(False)
        #BCM 모드로 핀 번호 다루기로 설정한다. BCM은 Broadcom 사의 SoC(System on Chip)에서 쓰는
        #넘버링 체계이다.
        GPIO.setmode(GPIO.BCM)
        #GPIO 핀 설정. 첫 parameter는 설정할 핀 번호, 두번째 것은 핀 모드. 23번 핀을 입력용으로 설정
        GPIO.setup(self.DIGIT,GPIO.IN)

        #spi 디바이스 기능을 쓰기 위해서 객체 생성.
        self.spi = spidev.SpiDev()
        #bus 열기. 첫 parameter는 버스 번호, 두번째 것은 device 번호
        self.spi.open(0,0)
        #spi 통신을 위한 최대 클럭 설정.
        self.spi.max_speed_hz=50000

    #Function to get ADC value
    def read_spi_adc(self, adcChannel):
        adcValue = 0
        buff = self.spi.xfer2( [1, (8+adcChannel)<<4, 0] )
        #spi transaction을 위한 설정. parameter는 list로써, MISO 핀으로 보낼 데이터.
        #transaction은 말 그대로 교환이기 때문에, 리스트에 담긴 무의미한(최소한 여기선) 데이터가 수분 센서로 보내지고,
        #그 대신 수분 센서 데이터가 반환된다.
        #리스트 외에 기타 데이터를 추가해서 transaction 설정을 바꿀 수 있지만, 입력 안했음. default 설정 사용.
        
        adcValue = ( (buff[1] & 3 ) << 8 ) + buff[2]
        #marshalling. 2byte의 데이터를 정렬해서 가져오기. 라즈베리파이에서는 word 당 비트 수가 8로 설정된다.
        #그래서, 8비트씩 쪼개서 가져온 걸 marshalling 한다.
        
        adcValue = 1023-adcValue # without this, when wet data's 0, dry data's 1023
        adcValue = adcValue*1.55  # without this max data is around 820.
        adcValue = int(adcValue)
        if adcValue > 1023:
            adcValue = 1023
        return adcValue

    #Map function to convert sensor's value into percentage.
    def map(self, value, min_adc, max_adc, min_hum, max_hum):
        adc_range = max_adc - min_adc
        hum_range = max_hum - min_hum
        scale_factor = float(adc_range) / float(hum_range)
        return min_hum + ( (value - min_adc) / scale_factor )

    def read_humidity(self):
        adcChannel = 0
        adcValue = self.read_spi_adc(adcChannel)
        #print("토양 수분 : %d" % (adcValue))
        digit_val = not(GPIO.input(self.DIGIT))
        #print("Digit Value : %d" % (digit_val))

        #converting received data into % measure.
        self.humidity = int(self.map(adcValue, 0, self.HUM_MAX, 0, 100))
        #print("측정된 습도 : %d%%" % (self.humidity))
        #time.sleep(0.5)
        return self.humidity

if __name__ == "__main__":
    import WaterSetting
    global waterSetting
    waterSetting = WaterSetting.WaterSetting()
    mH = MeasureHumidity()

    waterSetting.humidity = mH.read_humidity()
    print('waterSetting.lux :', waterSetting.humidity)


    

