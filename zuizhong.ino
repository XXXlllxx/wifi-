
#include <ESP8266WiFi.h>
#include <Ticker.h>
#include <Servo.h>
#include <DHT.h>

#define DHTPIN D2
#define DHTTYPE DHT11




#define MAX_SRV_CLIENTS 1            //定义可连接的客户端数目最大值
const char* ssid = "WulianWang_xx";           //账号
const char* password = "12345678";   //密码

WiFiServer server(8080);                    //端口号
WiFiClient serverClients[MAX_SRV_CLIENTS];  //客户端

uint8_t read_temp[60];

Ticker timer1;                            // 创建一个定时器对象
Servo Servo1;
Servo myservo;
DHT dht(DHTPIN, DHTTYPE);

int sensor=0;//电位器输入的类比讯号值
int angle=0;

float temperature;
float humidity;
float distance;
//float p;

const unsigned char pinMotorCW  = D5;   // 接控制电机顺时针转的 H 桥引脚
const unsigned char pinMotorCCW = D3;   // 接控制电机逆时针转的 H 桥引脚

void setup() {
 
	
  Serial.begin(115200); 
  WiFi.mode(WIFI_AP);           //AP模式
  WiFi.softAP(ssid, password);  //配置WIFI 
  IPAddress m_ip = WiFi.softAPIP();
  Serial.print("AP IP address: ");
  Serial.println(m_ip);
  
  server.begin();          //启动UART传输和服务器
  server.setNoDelay(true); //无发送延时

  pinMode(LED_BUILTIN, OUTPUT);
  Servo1.attach(D1);
  myservo.attach(D5);
   pinMode(pinMotorCW,  OUTPUT);
   pinMode(pinMotorCCW, OUTPUT);

  pinMode(12, OUTPUT);
  pinMode(D4, OUTPUT);
  pinMode(D5, INPUT);
  pinMode(D1,OUTPUT);

   dht.begin();

  // 设置周期性定时0.3s，即300ms，回调函数为timer1_cb，参数为LED引脚号，并启动定时器 
  timer1.attach(0.3, timer1_cb, LED_BUILTIN);
}

void loop() {

	uint8_t i;
	//检查连接
	if (server.hasClient()){//存在连接请求
		for(i = 0; i < MAX_SRV_CLIENTS; i++){
			//查找空的和已经断开连接的
			if (!serverClients[i] || !serverClients[i].connected()){
				//如果是空的
				if(!serverClients[i]){
					//停止连接
					serverClients[i].stop();
				} 
			//连接新的
			serverClients[i] = server.available();
			//提示连接成功
			Serial.print("CONNECTED"); 
			continue;
			} 
		}
	}
	//来自客户端的数据
	for(i = 0; i < MAX_SRV_CLIENTS; i++){
		if (serverClients[i] && serverClients[i].connected()){
			if(serverClients[i].available()){
				//从客户端获取数据
				while(serverClients[i].available()){
					//将数据转发到URAT端口
					//Serial.write(serverClients[i].read());
          serverClients[i].read(read_temp,60);


//     temperature = dht.readTemperature();
//     humidity = dht.readHumidity();
//     float p = checkdistance_4_5();
          if(read_temp[0] == 'k'){
              digitalWrite(LED_BUILTIN, LOW);

          }
          else if(read_temp[0] == 'g'){
             digitalWrite(LED_BUILTIN, HIGH);
          }
          //          模拟小车依靠舵机左转
           else if (read_temp[0] == 'f') {
            Servo1.write(0);
           
           }
//           模拟小车依靠舵机右转
           else if(read_temp[0] == 'r'){
            Servo1.write(180);
            
           }
//           模拟小车依靠舵机恢复直行方向
           else if(read_temp[0] == 'm'){
            Servo1.write(90);
            
           }
           else if (read_temp[0] == 'h') {
               digitalWrite(pinMotorCW,  HIGH);
               digitalWrite(pinMotorCCW, LOW);
           }
           else if(read_temp[0] == 's') {
             digitalWrite(pinMotorCW,  LOW);
             digitalWrite(pinMotorCCW, LOW);
           }
           else if(read_temp[0] == 'd') {
              digitalWrite(pinMotorCW,  LOW);
              digitalWrite(pinMotorCCW, HIGH);
           }
          else{

          }
          Serial.print(read_temp[60]);

				}       
			}
		}
	}
	//检查UART端口数据
	if(Serial.available()){
		size_t len = Serial.available();
		uint8_t sbuf[64];
		Serial.readBytes(sbuf, len);
		for(i = 0; i < MAX_SRV_CLIENTS; i++){
			if (serverClients[i] && serverClients[i].connected()){
				//将UART端口数据转发到已连接的客户端
				serverClients[i].write(sbuf, len);
				delay(1);
			}
		}
	}
   //temperature = dht.readTemperature();
   //humidity = dht.readHumidity();

}




float checkdistance_4_5() {
  digitalWrite(D4, LOW);
  delayMicroseconds(2);
  digitalWrite(D4, HIGH);
  delayMicroseconds(10);
  digitalWrite(D4, LOW);
  float distance = pulseIn(D5, HIGH) / 58.00;
}




//定时回调的函数
void timer1_cb(int led_pin) 
{
//  int state = digitalRead(led_pin);  // 获取当前led引脚状态
//  digitalWrite(led_pin, !state); // 翻转LED引脚电平
  int sensorValue = analogRead(A0);

// float h = temperature;
// float t = humidity;
  float p =  distance;

  if(p<20){
       tone(D8,523);
  }

  
  String valStr=String(p)+"cm/"+String(sensorValue);
  for(uint8_t i = 0; i < MAX_SRV_CLIENTS; i++){
    uint8_t sbuf[64] ;
    valStr.getBytes(sbuf,valStr.length()+1);  //string类型转成byte[]
    if (serverClients[i] && serverClients[i].connected()){
      //将数据转发到已连接的客户端
      serverClients[i].write(sbuf, valStr.length()+1);
      delay(1);
		}
	}

}
