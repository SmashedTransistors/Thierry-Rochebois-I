#include <_Teensy.h>

#include <Audio.h>
#include <MIDI.h>

#include "USBHost_t36.h"
#define LCALCBUF 32
#define NBVOX 8
#include <TiaR.h>
#include <TiaROne.h>
#include "Disp1106B.h"
#include "scope.h"

USBHost myusb;
USBHub hub1(myusb);
USBHub hub2(myusb);
USBHub hub3(myusb);
KeyboardController keyboard1(myusb);
KeyboardController keyboard2(myusb);
MIDIDevice_BigBuffer midi1(myusb);

tiar::Disp1106B disp;
tiar::Scope scope;
tiarone::APC25Receiver apcReceiver;
tiarone::APC25Display apcDisplay;
tiarone::Synth<NBVOX> synth;
tiarone::ReverbMS reverb;
tiarone::ChorusMS chorus;


float latestPitch=60;

// GUItool: begin automatically generated code
AudioPlayQueue           queue1;         //xy=228,342
AudioPlayQueue           queue3; //xy=240,428
AudioPlayQueue           queue2;         //xy=248,381
AudioPlayQueue           queue4; //xy=248,469
AudioOutputI2SQuad       i2s_quad1;      //xy=588,512
AudioConnection          patchCord1(queue1, 0, i2s_quad1, 0);
AudioConnection          patchCord3(queue2, 0, i2s_quad1, 1);
AudioConnection          patchCord2(queue3, 0, i2s_quad1, 2);
AudioConnection          patchCord4(queue4, 0, i2s_quad1, 3);
// GUItool: end automatically generated code
AudioControlSGTL5000  sgtl5000_1;

//MIDI TX1
MIDI_CREATE_INSTANCE(HardwareSerial, Serial1, MIDI);

float moyMicros=0;

float volume = 0;

int conso1=LCALCBUF;
int conso2=LCALCBUF;
int conso3=LCALCBUF;
int conso4=LCALCBUF;
float bufs[24*LCALCBUF];
float accBuf[LCALCBUF]; 
float accBufCh[LCALCBUF];
float accBufRv[LCALCBUF];
float accBufL[LCALCBUF],accBufR[LCALCBUF];
float accBufScope[LCALCBUF]; 
float chorusL[LCALCBUF],chorusR[LCALCBUF], reverbL[LCALCBUF], reverbR[LCALCBUF];

int16_t i16BufL[LCALCBUF];
int16_t i16BufR[LCALCBUF];
int16_t i16BufExtL[LCALCBUF];
int16_t i16BufExtR[LCALCBUF];
tiar::NoiseShaper nsL1,nsR1,nsL2,nsR2,nsExtL,nsExtR;
int32_t rnd=0;

String titre="TIAR - ONE";

void setup(){
  pinMode(2, INPUT_PULLUP);
  pinMode(3, INPUT_PULLUP);
  nsL1.rnd=137; nsR1.rnd=1246; nsL2.rnd=65465; nsR2.rnd=13134;
  delay(200);
	myusb.begin(); //alim usbHost
  delay(500);
  sgtl5000_1.enable();
  sgtl5000_1.volume(1.0);
  disp.setup(digitalRead(3)?1:0);
  disp.mode=0;
  disp.setText(0,titre);
  scope.init();
  
  usbMIDI.setHandleNoteOff(onNoteOffPc);
  usbMIDI.setHandleNoteOn(onNoteOnPc);
  usbMIDI.setHandleControlChange(onControlChangePc);
  usbMIDI.setHandleProgramChange(onProgramChangePc);
  usbMIDI.setHandlePitchChange(onPitchChangePc);

  MIDI.setHandlePitchBend(onPitchChangeDin);

  apcReceiver.common=&synth.common;
  apcReceiver.synth=&synth;
  apcDisplay.midi1 = &midi1; //utilisé pour envoyer les commandes
  synth.common.apcDisplay=&apcDisplay;

  synth.init(bufs, accBuf, accBufCh, accBufRv, &usbMIDI);
  reverb.init(accBufRv, reverbL, reverbR);
  chorus.init(accBufCh, chorusL, chorusR);
  conso1=LCALCBUF;
  conso2=LCALCBUF;
  conso3=LCALCBUF;
  conso4=LCALCBUF;

  AudioMemory(16);
  queue1.setMaxBuffers(3);
  queue2.setMaxBuffers(3);
  queue3.setMaxBuffers(3);
  queue4.setMaxBuffers(3);
  queue1.setBehaviour(AudioPlayQueue::NON_STALLING);
  queue2.setBehaviour(AudioPlayQueue::NON_STALLING);
  queue3.setBehaviour(AudioPlayQueue::NON_STALLING);
  queue4.setBehaviour(AudioPlayQueue::NON_STALLING);

  MIDI.begin(MIDI_CHANNEL_OMNI);

	midi1.setHandleNoteOff(onNoteOffAkai);
	midi1.setHandleNoteOn(onNoteOnAkai);
	midi1.setHandleControlChange(onControlChangeAkai);

  pinMode(LED_BUILTIN, OUTPUT);
  delay(200);
  digitalWrite(LED_BUILTIN, HIGH);
  synth.handleCC(0, 7, 96);//vol

  onProgramChangeDin(0,127); //se cale sur le dernier


}



int32_t kc=0;
void loop(){

  //MIDI HOST
  myusb.Task();
	while(midi1.read()){};

  




  //MIDI RX1
  while(MIDI.read()){ // look for a message
    switch(MIDI.getType()){ // get message type
      case midi::NoteOn:
        onNoteOnDin(MIDI.getChannel(), MIDI.getData1(),  MIDI.getData2());
        break;
      case midi::NoteOff:
        onNoteOffDin(MIDI.getChannel(), MIDI.getData1(),  MIDI.getData2());
        break;
        case midi::ControlChange:
        onControlChangeDin(MIDI.getChannel(), MIDI.getData1(),  MIDI.getData2());
                break;
        case midi::ProgramChange:
        onProgramChangeDin(MIDI.getChannel(), MIDI.getData1());
      default:
        break;
    }
  }

  //MIDI PC
  // compile USB Type :  serial + MIDI 
  while (usbMIDI.read()) {
    // read & ignore incoming messages
  }



  if(conso1 >= LCALCBUF && conso2 >= LCALCBUF && conso3 >= LCALCBUF && conso4 >= LCALCBUF){
    elapsedMicros micros;
    kc++;
    synth.bProc();
    for(int i=0;i<LCALCBUF;i++){
      accBufScope[i]=accBuf[i]+accBufCh[i]+accBufRv[i];  //no effect better display
    }
    reverb.bProc();
    chorus.bProc(synth.common.params2.ch);

    disp.mode=synth.common.baseDispMode==0?0:3;
    if(synth.common.detailCpt>=0){
      disp.mode=1;
      disp.setText(0,synth.common.lastMod);
    }

    disp.setBText(kc%8, synth.common.lines[kc%8]);


    // à modifier avec reverb et chorus
    for(int i=0;i<LCALCBUF;i++){
      accBufL[i]=accBuf[i]+chorusL[i]+reverbL[i];
      accBufR[i]=accBuf[i]+chorusR[i]+reverbR[i];
    }


    scope.bProc(accBufScope, latestPitch+12);
    disp.seqDraw(&scope);

    // float vol = volume*synth.common.muter.gain;

    float vol=q_to_float(synth.common.vol,27);
    vol=0.01f+0.54f*vol*vol; 
    volume+=0.1f*(vol-volume);


    //sorties différentielles
    if(digitalRead(2)){ //pulled up by default
      //sorties stéréos
      for(int i=0;i<LCALCBUF;i++){
        i16BufL[i]    =nsL1.quands(    volume*accBufL[i]);
        i16BufR[i]    =nsL2.quands(    volume*accBufR[i]);
        i16BufExtL[i] =nsR1.quands(    volume*accBufL[i]);
        i16BufExtR[i] =nsR2.quands(    volume*accBufR[i]);
      }  
    }else{ //differential outputs
      for(int i=0;i<LCALCBUF;i++){
        i16BufL[i]    =nsL1.quands(    volume*accBufL[i]);
        i16BufR[i]    =nsL2.quands(   -volume*accBufL[i]);
        i16BufExtL[i] =nsR1.quands(    volume*accBufR[i]);
        i16BufExtR[i] =nsR2.quands(   -volume*accBufR[i]);
      }
    }

  
    
    long tm=micros;
    
    if(kc==30) moyMicros=tm;
    else if(kc<4000){
      moyMicros+=0.001f*(tm-moyMicros);
    }else{
      moyMicros+=0.0001f*(tm-moyMicros);
    }
    if((kc&0xFFF)==0){
      if(Serial){
        if(digitalRead(2)){
          Serial.println("2 pulled up => standard stereo");
        } else{
          Serial.println("2 grounded => differential outputs");
        }
        if(digitalRead(3)){
          Serial.println("3 pulled up => SH1106");
        } else{
          Serial.println("3 grounded => SSD1309");
        }
        Serial.println("_________________________________");
        Serial.print(moyMicros, 3);
        Serial.print(" us   ");
        Serial.print(moyMicros*(1.0f/LCALCBUF), 3);
        Serial.println(" us/sample   ");
/*
        for(int i=0;i<8;i++){
          Serial.println(synth.common.lines[i]);
        }*/

       /* for(int i=0;i<27;i++)
          Serial.printf("gA[%d]=%d ",i,synth.voxes[0].osc.gA[i]);
        Serial.println();*/
        /* Serial.printf("osc.slA.op0.dp=%d \n",synth.voxes[0].osc.slA.op0.dp);
         Serial.printf("osc.slA.op1.dp=%d \n",synth.voxes[0].osc.slA.op1.dp);
         Serial.printf("osc.slA.op2.dp=%d \n",synth.voxes[0].osc.slA.op2.dp);
         Serial.printf("osc.slA.op0.p=%d \n",synth.voxes[0].osc.slA.op0.p);
         Serial.printf("osc.slA.op1.p=%d \n",synth.voxes[0].osc.slA.op1.p);
         Serial.printf("osc.slA.op2.p=%d \n",synth.voxes[0].osc.slA.op2.p);
         Serial.printf("osc.slA.op0.y=%d \n",synth.voxes[0].osc.slA.op0.y);
         Serial.printf("osc.slA.op1.y=%d \n",synth.voxes[0].osc.slA.op1.y);
         Serial.printf("osc.slA.op2.y=%d \n",synth.voxes[0].osc.slA.op2.y);
         Serial.printf("osc.slA.out=%d \n",synth.voxes[0].osc.slA.out);
         Serial.printf("osc.slA.outCh=%d \n",synth.voxes[0].osc.slA.outCh);
         Serial.printf("osc.slA.out=%d \n",synth.voxes[0].osc.slB.out);
         Serial.printf("osc.slA.outCh=%d \n",synth.voxes[0].osc.slB.outCh);*/
        
      }
    }
    conso1=0;
    conso2=0;
    conso3=0;
    conso4=0;
  }
  
  if(conso1<LCALCBUF) 
    conso1 += queue1.play(i16BufL[conso1])==0?1:0;

  if(conso2<LCALCBUF) 
    conso2 += queue2.play(i16BufR[conso2])==0?1:0;
  if(conso3<LCALCBUF) 
    conso3 += queue3.play(i16BufExtL[conso3])==0?1:0;
  if(conso4<LCALCBUF) 
    conso4 += queue4.play(i16BufExtR[conso4])==0?1:0;



}

void onNoteOnPc(byte channel, byte note, byte velocity)
{
  if(velocity!=0) 
    latestPitch=note;
  synth.noteOn(channel,note,velocity);
  
  Serial.printf("note=%d\n",note);
}
void onNoteOnDin(byte channel, byte note, byte velocity)
{
  if(velocity!=0) 
    latestPitch=note;
  synth.noteOn(channel,note,velocity); 
  Serial.printf("note=%d\n",note);
}




void onNoteOnAkai(byte channel, byte note, byte velocity)
{
  int cable = midi1.getCable();
  if(cable==0){
    synth.noteOn(channel,note,velocity);
    usbMIDI.sendNoteOn(note, velocity, channel); // up to the PC
    Serial.printf("usbMIDI.sendNoteOn %d %d %d,\n",note, velocity, channel);
    if(velocity!=0) {
      latestPitch=note;
    }
  } else {
    if(note==98){
      apcReceiver.setShift(velocity>0?true:false);
    }else{
      apcReceiver.handlePad(note);
    }
  }
  Serial.printf("USBHost Note ch=%d cable=%d, note=%d, vel=%d\n",channel,midi1.getCable(),note,velocity);
}
//______________________________________________________________________
void onNoteOffPc(byte channel, byte note, byte velocity)
{
  synth.noteOff(channel,note,velocity);
}
void onNoteOffDin(byte channel, byte note, byte velocity)
{
  synth.noteOff(channel,note,velocity);
}
void onNoteOffAkai(byte channel, byte note, byte velocity)
{
  int cable = midi1.getCable();
  if(cable==0){
    synth.noteOff(channel,note,velocity);
    usbMIDI.sendNoteOff(note, velocity, channel); // up to the PC
  } else {
    if(note==98){
      apcReceiver.setShift(false);
    }
  }
}
//______________________________________________________________________

void onControlChangePc(byte channel, byte control, byte value)
{
  if(Serial){
    Serial.printf("CC, ch=%d control=%d, value=%d\n",channel,control,value);
  }
  synth.handleCC(channel, control, value);
}
void onControlChangeDin(byte channel, byte control, byte value)
{
  if(Serial){
    Serial.printf("CC, ch=%d control=%d, value=%d\n",channel,control,value);
  }
  synth.handleCC(channel, control, value);
}
void onControlChangeAkai(byte channel, byte control, byte value)
{
  if(Serial){
    Serial.printf("AkaiOnControlChange, ch=%d cable=%d, control=%d, value=%d\n",channel,midi1.getCable(),control,value);
   /* for(int i=0;i<8;i++){
      Serial.printf("k[%d]=%f\n",i,((float)apc25.k[i])/(1<<21));
    }*/
  }
  apcReceiver.handleKnob(control, value);
  Serial.printf("synth.common.params2.fCut.val = %d\n", synth.common.params2.fCut.val);
  Serial.printf("synth.common.params2.fMod.val = %d\n", synth.common.params2.fMod.val);
  Serial.printf("synth.common.params2.fEnv.val = %d\n", synth.common.params2.fEnv.val);
  Serial.printf("synth.common.params2.vEnv.val = %d\n", synth.common.params2.vEnv.val);
  Serial.printf("synth.common.params2.mrph.val = %d\n", synth.common.params2.mrph.val);
  Serial.printf("synth.common.params2.mMod.val = %d\n", synth.common.params2.mMod.val);
  Serial.printf("synth.common.params2.mRet.val = %d\n", synth.common.params2.mRat.val);
  Serial.printf("synth.common.params2.vol.val = %d\n", synth.common.params2.vol.val);
 
}
//______________________________________________________________________

void onProgramChangePc(byte channel, byte n) {
  synth.handleProgramChange(channel, n);
}
void onProgramChangeDin(byte channel, byte n) {
  synth.handleProgramChange(channel, n);
}
//______________________________________________________________________
void onPitchChangePc(byte channel, int value)
{
  if(Serial){
    Serial.printf("PitchChange, ch=%d value=%d\n",channel,value);
  }
  synth.HandlePitchBend(channel, value);
}
void onPitchChangeDin(byte channel, int value)
{
  if(Serial){
    Serial.printf("PitchChange, ch=%d value=%d\n",channel,value);
  }
  synth.HandlePitchBend(channel, value);
}




