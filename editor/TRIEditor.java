import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import static javax.sound.midi.ShortMessage.CONTROL_CHANGE;

public class TRIEditor extends JPanel implements Receiver {

    public static final Color REV_OFF = new Color(200, 160, 160);
    public static final Color REV_ON = Color.red;
    public static final Color FUNC_OFF = new Color(180, 170, 230);
    public static final Color FUNC_ON = new Color(220, 90, 255);

    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (message.getMessage()[0] != -65) {
            return;
        }
        int control = message.getMessage()[1];
        int value = message.getMessage()[2];

        switch (control) {
            case 74: {
                getCurrentPreset().fCut = value;
            }
            break;
            case 73: {
                getCurrentPreset().fMod = value;
            }
            break;
            case 72: {
                getCurrentPreset().fEnv = value;
            }
            break;
            case 16: {
                getCurrentPreset().vEnv = value;
            }
            break;
            case 1: {
                getCurrentPreset().mrph = value;
            }
            break;
            case 18: {
                getCurrentPreset().mMod = value;
            }
            break;
            case 19: {
                getCurrentPreset().mRat = value;
            }
            break;

            case 91: {
                getCurrentPreset().rev = Math.min(value, 7);
            }
            break;
            case 93: {
                getCurrentPreset().ch = Math.min(value, 3);
            }
            break;
            case 94: {
                getCurrentPreset().sym = Math.min(value, 7);
            }
            break;
            case 71: {
                getCurrentPreset().fRes = Math.min(value, 5);
            }
            break;
            case 78: {
                getCurrentPreset().a1 = value < 32 ? value : 31;
            }
            break;
            case 79: {
                getCurrentPreset().b1 = value < 33 ? value : 32;
            }
            break;
            case 75: {
                getCurrentPreset().a0 = value < 32 ? value : 31;
            }
            break;
            case 76: {
                getCurrentPreset().b0 = value < 33 ? value : 32;
            }
            break;
            case 77: {
                getCurrentPreset().mFunc = value < 8 ? value : 7;
            }
            break;
        }
        updateGUIfromPreset();

    }

    @Override
    public void close() {

    }

    static class Preset {
        public Preset(String name, int fCut, int fMod, int fEnv, int fRes, int vEnv, int mrph, int mMod, int mRat, int a1, int b1, int mFunc, int a0, int b0, int sym, int rev, int ch) {
            this.name = name;
            this.fCut = fCut;
            this.fMod = fMod;
            this.fEnv = fEnv;
            this.fRes = fRes;
            this.vEnv = vEnv;
            this.mrph = mrph;
            this.mMod = mMod;
            this.mRat = mRat;
            this.a1 = a1;
            this.b1 = b1;
            this.mFunc = mFunc;
            this.a0 = a0;
            this.b0 = b0;
            this.sym = sym;
            this.rev = rev;
            this.ch = ch;
        }

        public Preset() {
        }

        String name = "basic";
        int fCut = 127, fMod = 127, fEnv = 127, fRes = 0,
                vEnv, mrph, mMod, mRat, a1, b1, mFunc, a0, b0, sym, rev, ch;

        public String toCsv() {
            return String.format("%12s,", name)
                    + String.format("  %3d,  %3d,  %3d,  %3d,  %3d,", fCut, fMod, fEnv, fRes, vEnv)
                    + String.format("  %3d,  %3d,  %3d,", mrph, mMod, mRat)
                    + String.format(" %2d, %2d,   %3d, %2d, %2d,", a1, b1, mFunc, a0, b0)
                    + String.format(" %3d, %3d, %2d", sym, rev, ch);
        }

        public static String csvTitles() {
            return "Preset name " +
                    ", fCut, fMod, fEnv, fRes, vEnv" +
                    ", mrph, mMod, mRat" +
                    ", a1, b1, mFunc, a0, b0" +
                    ", sym" + ", rev" + ", ch";
        }
    }

    int numCurrentPreset = 0;

    Preset getCurrentPreset() {
        return presets[numCurrentPreset];
    }

    Preset[] presets = new Preset[40];

    //active pads
    int litPadA0, stateA0;
    int litPadA1, stateA1;
    int litPadB0, stateB0;
    int litPadB1, stateB1;
    int litPadLFF;

    DefaultListModel<String> listModelMidiIn = new DefaultListModel<>();
    DefaultListModel<String> listModelMidiOut = new DefaultListModel<>();

   // JTextField tPresetFile = new JTextField(); //TODO Pas utile => on demande systématiquement ?
   // JButton bLoadFromFile = new JButton("Load Preset from File");
   // JButton bSaveToFile = new JButton("Save Preset File To...");
    JButton bGetFromTeensy = new JButton("Get Preset from Teensy");
    JSpinner spGetFromTeensy = new JSpinner(new SpinnerNumberModel(0, 0, 127, 1));
    JButton bSaveToTeensy = new JButton("Save Preset to Teensy");
    JSpinner spSaveToTeensy = new JSpinner(new SpinnerNumberModel(0, 0, 127, 1));


    JList<String> jlMidiOuts;
    MidiDevice mdMidiOut = null;
    Receiver rcMidiOut = null;

    JList<String> jlMidiIns;
    MidiDevice mdMidiIn = null;
    Transmitter trMidiIn = null;


 //   DefaultListModel<String> listModelPresets = new DefaultListModel<>();
 //   JList<String> jlPresets;


    JButton[] bPads = new JButton[40];
    JButton[] bRev = new JButton[8];
    JButton[] bReso = new JButton[6];
    JSlider sFCut = new JSlider(JSlider.VERTICAL, 0, 127, 127);
    JSlider sFMod = new JSlider(JSlider.VERTICAL, -64, 63, 63);
    JSlider sFEnv = new JSlider(JSlider.VERTICAL, 0, 127, 127);

    JSlider sMrph = new JSlider(JSlider.VERTICAL, 0, 127, 0);
    JSlider sMMod = new JSlider(JSlider.VERTICAL, 0, 127, 0);
    JSlider sMRat = new JSlider(JSlider.VERTICAL, 0, 127, 0);

    JSlider sVEnv = new JSlider(JSlider.VERTICAL, 0, 127, 0);
    JSlider sVol = new JSlider(JSlider.VERTICAL, 0, 127, 127);

    DefaultListModel<String> listModelChorus = new DefaultListModel<>();
    JList<String> jlChorus;

    DefaultListModel<String> listModelSym = new DefaultListModel<>();
    JList<String> jlSym;

    private void initPresets() {
        presets[0] = new Preset("bass", 0, 127, 32, 2, 0, 0, 81, 40, 2, 2, 0, 0, 1, 0, 2, 1);
        presets[1] = new Preset("china", 27, 127, 70, 4, 30, 0, 127, 51, 1, 32, 0, 4, 6, 1, 3, 3);
        presets[2] = new Preset("cordéon ", 7, 25, 55, 1, 94, 33, 12, 98, 7, 6, 4, 5, 32, 0, 3, 1);
        presets[3] = new Preset("west ", 27, 127, 127, 0, 0, 0, 54, 101, 5, 7, 4, 5, 32, 0, 4, 0);
        presets[4] = new Preset("sync ", 27, 127, 127, 0, 0, 32, 61, 31, 8, 0, 0, 5, 4, 0, 2, 3);
        presets[5] = new Preset("sync harp", 49, 127, 48, 1, 0, 0, 54, 28, 9, 32, 0, 6, 2, 2, 4, 0);
        presets[6] = new Preset("sync dream", 30, 64 + 36, 21, 0, 31, 64, 64, 40, 2, 8, 2, 11, 6, 0, 5, 2);
        presets[7] = new Preset("phaser", 91, 0, 76, 2, 110, 0, 14, 25, 8, 8, 4, 6, 6, 0, 5, 1);
        presets[8] = new Preset("clav1", 0, 64 + 40, 70, 1, 0, 0, 43, 50, 14, 32, 0, 12, 32, 7, 2, 3);
        presets[9] = new Preset("clav2", 0, 64 + 40, 70, 1, 0, 0, 43, 50, 15, 32, 0, 12, 32, 7, 2, 3);
        presets[10] = new Preset("FM Bass", 127, 64 + 63, 127, 0, 6, 127, 127, 70, 14, 14, 0, 12, 12, 4, 2, 2);
        presets[11] = new Preset("space", 100, 0, 121, 4, 51, 0, 14, 93, 14, 15, 4, 12, 12, 3, 5, 3);
        presets[12] = new Preset("harsh1", 100, 64 - 13, 73, 0, 14, 75, 74, 60, 17, 10, 2, 16, 4, 0, 3, 0);
        presets[13] = new Preset("haunted", 127, 0, 43, 0, 108, 0, 48, 25, 10, 14, 4, 13, 4, 1, 3, 2);
        presets[14] = new Preset("Baroque", 127, 0, 0, 0, 0, 64, 24, 0, 14, 17, 7, 0, 32, 3, 4, 2);
        presets[15] = new Preset("Horror", 127, 64 + 63, 127, 0, 29, 127, 127, 33, 13, 32, 2, 7, 7, 7, 3, 1);
        presets[16] = new Preset("Horror Pad", 47, 64, 127, 0, 62, 86, 54, 32, 17, 11, 4, 12, 5, 0, 4, 1);
        presets[17] = new Preset("Horror Vox", 127, 64 + 54, 127, 0, 23, 109, 30, 9, 29, 0, 4, 0, 28, 4, 7, 0);
        presets[18] = new Preset("Crazy Robot", 127, 64 + 54, 127, 0, 0, 0, 127, 89, 8, 15, 7, 0, 32, 0, 1, 0);
        presets[19] = new Preset("Dry Slap", 10, 64 + 61, 22, 1, 0, 10, 44, 93, 21, 0, 0, 4, 0, 4, 1, 0);
        presets[20] = new Preset("Forbidden Pl", 17, 64 + 63, 81, 4, 61, 107, 12, 127, 8, 32, 4, 20, 21, 1, 6, 0);
        presets[21] = new Preset("Nice noise", 38, 64, 0, 3, 0, 97, 0, 74, 28, 5, 0, 20, 5, 0, 2, 3);
        presets[22] = new Preset("Broken", 0, 64 + 63, 30, 1, 25, 97, 80, 16, 28, 25, 4, 20, 24, 0, 3, 0);
        presets[23] = new Preset("Solino", 127, 64 + 63, 127, 0, 110, 64, 100, 29, 6, 3, 4, 4, 7, 3, 3, 2);
        presets[24] = new Preset("1988", 0, 64 + 63, 0, 2, 28, 28, 0, 29, 15, 25, 4, 12, 6, 3, 2, 2);
        presets[25] = new Preset("evening", 116, 64 + 13, 64, 0, 70, 100, 0, 28, 15, 25, 0, 1, 28, 0, 5, 2);
        presets[26] = new Preset("SF", 127, 0, 28, 2, 36, 0, 127, 34, 2, 7, 0, 0, 28, 3, 5, 1);
        presets[27] = new Preset("West Terne", 127, 64 + 63, 127, 0, 0, 127, 94, 23, 7, 24, 0, 22, 23, 1, 4, 0);
        presets[28] = new Preset("Classic Phas", 127, 64 + 63, 127, 0, 0, 127, 92, 32, 7, 7, 4, 2, 2, 4, 5, 0);
        presets[29] = new Preset("From the sky", 127, 64 + 63, 40, 0, 0, 0, 127, 0, 23, 15, 0, 28, 11, 1, 5, 0);
        presets[30] = new Preset("Basic", 127, 64 + 63, 127, 0, 0, 0, 0, 32, 0, 0, 0, 0, 0, 0, 0, 1);
        for (int i = 0; i < 40; i++) {
            presets[i] = new Preset();
        }

    }

    private void updatePadColors() {
        Color[] c = {Color.green, Color.orange, Color.red, Color.cyan, Color.white};
        deduce();
        for (int numPad = 0; numPad < 8; numPad++) {
            if (numPad != litPadA1) {
                bPads[numPad].setBackground(Color.lightGray);
            } else {
                bPads[numPad].setBackground(c[stateA1]);
            }
        }
        for (int numPad = 8; numPad < 16; numPad++) {
            int np = numPad - 8;
            if (np != litPadB1) {
                bPads[numPad].setBackground(Color.lightGray);
            } else {
                bPads[numPad].setBackground(c[stateB1]);
            }
        }
        for (int numPad = 16; numPad < 24; numPad++) {
            int np = numPad - 16;
            if (np != litPadLFF) {
                bPads[numPad].setBackground(FUNC_OFF);
            } else {
                bPads[numPad].setBackground(FUNC_ON);
            }
        }
        for (int numPad = 24; numPad < 32; numPad++) {
            int np = numPad - 24;
            if (np != litPadA0) {
                bPads[numPad].setBackground(Color.lightGray);
            } else {
                bPads[numPad].setBackground(c[stateA0]);
            }
        }
        for (int numPad = 32; numPad < 40; numPad++) {
            int np = numPad - 32;
            if (np != litPadB0) {
                bPads[numPad].setBackground(Color.lightGray);
            } else {
                bPads[numPad].setBackground(c[stateB0]);
            }
        }
    }

    private void updateRevColors() {
        for (int numRev = 0; numRev < 8; numRev++) {
            if (numRev != getCurrentPreset().rev) {
                bRev[numRev].setBackground(REV_OFF);
            } else {
                bRev[numRev].setBackground(REV_ON);
            }
        }
    }

    private void updateResoColors() {
        Color resoOff = new Color(200, 230, 200);
        for (int numReso = 0; numReso < 6; numReso++) {
            if (numReso != 5 - getCurrentPreset().fRes) {
                bReso[numReso].setBackground(resoOff);
            } else {
                bReso[numReso].setBackground(Color.green);
            }
        }
    }

    ArrayList<String> midiDeviceReceiverNames;
    ArrayList<MidiDevice> midiDeviceReceivers;
    ArrayList<String> midiDeviceTransmitterNames;
    ArrayList<MidiDevice> midiDeviceTransmitters;

    void sendCC(int cc, int val) {
        if (rcMidiOut == null) {
            return;
        }
        ShortMessage m = new ShortMessage();
        try {
            m.setMessage(CONTROL_CHANGE, 0, cc, val);
            rcMidiOut.send(m, -1);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }
    void sendPC(int pc) {
        if (rcMidiOut == null) {
            return;
        }
        try {
            ShortMessage m = new ShortMessage(ShortMessage.PROGRAM_CHANGE,
                    0, pc, 0);
            rcMidiOut.send(m, -1);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    void sendNoteOn(int note, int vel) {
        if (rcMidiOut == null) {
            return;
        }
        ShortMessage m = new ShortMessage();
        try {
            m.setMessage(ShortMessage.NOTE_ON, 0, note, vel);
            rcMidiOut.send(m, -1);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }


    void scanMidiInDevices() {
        midiDeviceTransmitterNames = new ArrayList<>();
        midiDeviceTransmitters = new ArrayList<>();
        //MIDI
        MidiDevice.Info[] deviceInfo = MidiSystem.getMidiDeviceInfo();
        if (deviceInfo.length == 0) {
            System.out.println("No MIDI devices found");
            return;
        }

        for (MidiDevice.Info info : deviceInfo) {
            System.out.println("**********************");
            System.out.println("Device name: " + info.getName());
            System.out.println("Description: " + info.getDescription());
            System.out.println("Vendor: " + info.getVendor());
            System.out.println("Version: " + info.getVersion());

            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device.getMaxTransmitters() != 0) {
                    midiDeviceTransmitterNames.add(info.getName());
                    midiDeviceTransmitters.add(device);
                }
                printDeviceType(device);
                System.out.println("Maximum receivers: "
                        + maxToString(device.getMaxReceivers()));
                System.out.println("Maximum transmitters: "
                        + maxToString(device.getMaxTransmitters()));
            } catch (MidiUnavailableException e) {
                System.out.println("Can't get MIDI device");
            }
        }

        listModelMidiIn.clear();
        for (String n : midiDeviceTransmitterNames) {
            listModelMidiIn.addElement(n);
        }

    }

    void scanMidiOutDevices() {
        midiDeviceReceiverNames = new ArrayList<>();
        midiDeviceReceivers = new ArrayList<>();
        //MIDI
        MidiDevice.Info[] deviceInfo = MidiSystem.getMidiDeviceInfo();
        if (deviceInfo.length == 0) {
            System.out.println("No MIDI devices found");
            return;
        }

        for (MidiDevice.Info info : deviceInfo) {
            System.out.println("**********************");
            System.out.println("Device name: " + info.getName());
            System.out.println("Description: " + info.getDescription());
            System.out.println("Vendor: " + info.getVendor());
            System.out.println("Version: " + info.getVersion());

            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device.getMaxReceivers() != 0) {
                    midiDeviceReceiverNames.add(info.getName());
                    midiDeviceReceivers.add(device);
                }

                printDeviceType(device);
                System.out.println("Maximum receivers: "
                        + maxToString(device.getMaxReceivers()));
                System.out.println("Maximum transmitters: "
                        + maxToString(device.getMaxTransmitters()));
            } catch (MidiUnavailableException e) {
                System.out.println("Can't get MIDI device");
            }
        }
        listModelMidiOut.clear();
        for (String n : midiDeviceReceiverNames) {
            listModelMidiOut.addElement(n);
        }
    }

    void autoSelectTeensy() {
        for (int i = 0; i < listModelMidiIn.getSize(); i++) {
            if ((listModelMidiIn.get(i)).startsWith("Teensy")) {
                jlMidiIns.setSelectedIndex(i);
            }
        }
        for (int i = 0; i < listModelMidiOut.getSize(); i++) {
            if ((listModelMidiOut.get(i)).startsWith("Teensy")) {
                jlMidiOuts.setSelectedIndex(i);
            }
        }
    }

    JPanel slPanel(JSlider sl, String label, Color labelColor) {
        JPanel panel = new JPanel();
        BoxLayout bl = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(bl);

        sl.setMajorTickSpacing(16);
        sl.setPaintTicks(true);
        sl.setPaintLabels(true);
        JLabel lbl = new JLabel(label, JLabel.LEFT);
        lbl.setForeground(labelColor);
        panel.add(lbl);
        panel.add(sl);
        return panel;
    }

    // Method
    TRIEditor() {
        JPanel comSelectors = new JPanel();
        {   //input device
            scanMidiInDevices();
            JPanel jpMidiIns = new JPanel();
            BoxLayout blMidiIns = new BoxLayout(jpMidiIns, BoxLayout.Y_AXIS);
            jpMidiIns.setLayout(blMidiIns);

            //Create the list and put it in a scroll pane.
            jlMidiIns = new JList<>(listModelMidiIn);
            jlMidiIns.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jlMidiIns.setSelectedIndex(0);
            jlMidiIns.setVisibleRowCount(10);
            jlMidiIns.addListSelectionListener(this::midiInSelected);
            jpMidiIns.add(new JLabel("MIDI Teensy->PC"));
            jpMidiIns.add(new JScrollPane(jlMidiIns));
            comSelectors.add(jpMidiIns);
        }
        {   //output device
            scanMidiOutDevices();
            JPanel jpMidiOuts = new JPanel();
            BoxLayout blMidiOuts = new BoxLayout(jpMidiOuts, BoxLayout.Y_AXIS);
            jpMidiOuts.setLayout(blMidiOuts);

            //Create the list and put it in a scroll pane.
            jlMidiOuts = new JList<>(listModelMidiOut);
            jlMidiOuts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jlMidiOuts.setSelectedIndex(0);
            jlMidiOuts.setVisibleRowCount(10);
            jlMidiOuts.addListSelectionListener(this::midiOutSelected);
            jpMidiOuts.add(new JLabel("MIDI PC->Teensy"));
            jpMidiOuts.add(new JScrollPane(jlMidiOuts));
            comSelectors.add(jpMidiOuts);
        }
        autoSelectTeensy();
        this.add(comSelectors);

        JPanel librarian = new JPanel();

        {   //librarian buttons
            JPanel panel = new JPanel();
            GridBagLayout bl = new GridBagLayout();
            panel.setLayout(bl);
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(3, 5, 3, 5);
            c.fill = GridBagConstraints.HORIZONTAL;
            //    panel.add(tPresetFile);
            //    bLoadFromFile.addActionListener(this::loadClicked);
            //    panel.add(bLoadFromFile);
            //    panel.add(bSaveToFile);

            bGetFromTeensy.addActionListener(this::getFromTeensy);
            c.gridx = 0;
            c.gridy = 0;
            panel.add(bGetFromTeensy, c);
            c.gridx = 1;
            panel.add(spGetFromTeensy, c);
            bSaveToTeensy.addActionListener(this::saveToTeensy);
            c.gridy = 1;
            c.gridx = 0;
            panel.add(bSaveToTeensy, c);
            c.gridx = 1;
            panel.add(spSaveToTeensy, c);


            librarian.add(panel);
        }


        {   //current presets
            initPresets();
          /*  listModelPresets.clear();
            int i = 0;
            for (Preset p : presets) {
                listModelPresets.addElement(String.format("%02d: %s", i, p.name));
                i++;
            }

            jlPresets = new JList<>(listModelPresets);
            jlPresets.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jlPresets.setSelectedIndex(0);
            jlPresets.setVisibleRowCount(10);
            jlPresets.addListSelectionListener(this::presetSelected);
            librarian.add(new JScrollPane(jlPresets));*/
        }
        this.add(librarian);


        JPanel controls = new JPanel();

        sFCut.addChangeListener(this::cutoffChanged);
        controls.add(slPanel(sFCut, "FCut", Color.red));
        sFMod.addChangeListener(this::fModChanged);
        controls.add(slPanel(sFMod, "FMod", Color.red));
        sFEnv.addChangeListener(this::fEnvChanged);
        controls.add(slPanel(sFEnv, "FEnv", Color.red));

        sMrph.addChangeListener(this::mrphChanged);
        controls.add(slPanel(sMrph, "Mrph", Color.orange.darker()));
        sMMod.addChangeListener(this::mModChanged);
        controls.add(slPanel(sMMod, "MMod", Color.orange.darker()));
        sMRat.addChangeListener(this::mRateChanged);
        controls.add(slPanel(sMRat, "MRate", Color.orange.darker()));

        sVEnv.addChangeListener(this::vEnvChanged);
        controls.add(slPanel(sVEnv, "VEnv", Color.black));
        sVol.addChangeListener(this::volChanged);
        controls.add(slPanel(sVol, "Vol", Color.black));

        //Create the list and put it in a scroll pane.
        listModelChorus.addElement("Chorus 0");
        listModelChorus.addElement("Chorus 1");
        listModelChorus.addElement("Chorus 2");
        listModelChorus.addElement("Chorus 3");
        jlChorus = new JList<>(listModelChorus);
        jlChorus.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jlChorus.setSelectedIndex(0);
        jlChorus.addListSelectionListener(this::chorusSelected);
        //jlMidiOuts.addListSelectionListener(this);
        jlChorus.setVisibleRowCount(4);

        listModelSym.addElement("Tri 1/2");
        listModelSym.addElement("Tri 1/3");
        listModelSym.addElement("Tri 1/4");
        listModelSym.addElement("Tri .49");
        listModelSym.addElement("Sin 1/2");
        listModelSym.addElement("Sin 1/3");
        listModelSym.addElement("Sin 1/4");
        listModelSym.addElement("Sin .49");
        jlSym = new JList<>(listModelSym);
        jlSym.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jlSym.setSelectedIndex(0);
        jlSym.addListSelectionListener(this::symSelected);
        //jlMidiOuts.addListSelectionListener(this);
        jlSym.setVisibleRowCount(8);


        controls.add(jlChorus);
        controls.add(jlSym);

        this.add(controls);


        JPanel grPads = new JPanel();
        GridBagLayout gblPads = new GridBagLayout();
        grPads.setLayout(gblPads);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 5, 3, 5);
        String[] colNames = {"", "SAW", "STR", "SYN", "PLK", "DXW", "TOY", "GRG", "END", "Reso"};
        for (int col = 0; col < 10; col++) {

            JLabel l = new JLabel(colNames[col], JLabel.CENTER);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = col;
            c.gridy = 0;
            grPads.add(l, c);
        }
        String[] rowNames = {"Morph A1", "Morph B1", "Func", "Morph A0", "Morph B0"};
        String[] funcNames = {"Decay", "LFO", "Dec+LFO", "Dec+fLFO", "comLFO", "LFOs", "velDec", "rand"};
        for (int row = 0; row < 5; row++) {
            JLabel l = new JLabel(rowNames[row]);
            c.gridy = row + 1;
            c.gridx = 0;
            c.insets = new Insets(3, 50, 3, 0);
            grPads.add(l, c);
            for (int col = 0; col < 8; col++) {

                int numPad = row * 8 + col;
                if (row != 2) {
                    bPads[numPad] = new JButton();
                } else {
                    bPads[numPad] = new JButton(funcNames[col]);
                }
                bPads[numPad].setPreferredSize(new Dimension(70, 35));
                bPads[numPad].setMargin(new Insets(0, 0, 0, 0));
                bPads[numPad].setName(String.format("%d", numPad));
                bPads[numPad].addActionListener(this::padClicked);
                c.gridx = col + 1;
                c.insets = new Insets(3, 5, 3, 5);
                grPads.add(bPads[numPad], c);
            }
            //reso
            bReso[row] = new JButton();
            bReso[row].setPreferredSize(new Dimension(25, 25));
            bReso[row].setName(String.format("%d", row));
            bReso[row].addActionListener(this::resoClicked);
            c.gridx = 9;
            c.insets = new Insets(5, 30, 5, 30);
            grPads.add(bReso[row], c);
        }
        //reverb buttons
        {
            JLabel l = new JLabel("Reverb");
            c.gridy = 6;
            c.gridx = 0;
            c.insets = new Insets(5, 30, 5, 30);

            grPads.add(l, c);
            for (int col = 0; col < 8; col++) {
                bRev[col] = new JButton();
                bRev[col].setPreferredSize(new Dimension(25, 25));
                bRev[col].setMargin(new Insets(5, 5, 5, 5));
                bRev[col].setName(String.format("%d", col));
                bRev[col].addActionListener(this::revClicked);
                c.gridx = col + 1;
                grPads.add(bRev[col], c);
            }
        }
        bReso[5] = new JButton("");
        bReso[5].setPreferredSize(new Dimension(25, 25));
        bReso[5].setName(String.format("%d", 5));
        bReso[5].addActionListener(this::resoClicked);
        c.gridx = 9;
        c.gridy = 6;
        c.insets = new Insets(5, 30, 5, 30);
        grPads.add(bReso[5], c);

        updatePadColors();
        updateRevColors();
        updateResoColors();
        this.add(grPads);


        updateGUIfromPreset();
        sendPreset(getCurrentPreset());

        {
            //keyboard
            JPanel kb = new JPanel();
            GridBagLayout bl = new GridBagLayout();
            kb.setLayout(bl);
            GridBagConstraints bc = new GridBagConstraints();
            bc.insets = new Insets(2, 1, 2, 1);
            bc.gridheight = 1;
            for(int oct = 0;oct<7;oct++) {
                {
                    bc.gridwidth = 1;
                    bc.gridx = 0 + 14 * oct;
                    bc.gridy = 0;
                    JLabel l = new JLabel(" ");
                    l.setPreferredSize(new Dimension(8, 32));
                    kb.add(l, bc);
                }
                bc.gridwidth = 2;
                addKeyToKb(0 + 12 * oct, 0 + 14 * oct, 1, bc, kb);
                addKeyToKb(1 + 12 * oct, 1 + 14 * oct, 0, bc, kb);
                addKeyToKb(2 + 12 * oct, 2 + 14 * oct, 1, bc, kb);
                addKeyToKb(3 + 12 * oct, 3 + 14 * oct, 0, bc, kb);
                addKeyToKb(4 + 12 * oct, 4 + 14 * oct, 1, bc, kb);
                {
                    bc.gridwidth = 1;
                    bc.gridx = 6 + 14 * oct;
                    bc.gridy = 0;
                    JLabel l = new JLabel(" ");
                    l.setPreferredSize(new Dimension(8, 32));
                    kb.add(l, bc);
                }
                bc.gridwidth = 2;
                addKeyToKb(5 + 12 * oct, 6 + 14 * oct, 1, bc, kb);
                addKeyToKb(6 + 12 * oct, 7 + 14 * oct, 0, bc, kb);
                addKeyToKb(7 + 12 * oct, 8 + 14 * oct, 1, bc, kb);
                addKeyToKb(8 + 12 * oct, 9 + 14 * oct, 0, bc, kb);
                addKeyToKb(9 + 12 * oct, 10 + 14 * oct, 1, bc, kb);
                addKeyToKb(10 + 12 * oct, 11 + 14 * oct, 0, bc, kb);
                addKeyToKb(11 + 12 * oct, 12 + 14 * oct, 1, bc, kb);
            }
            this.add(kb);
        }


        this.setVisible(true);
    }

    private void addKeyToKb(int n, int x, int y, GridBagConstraints bc, JPanel kb) {
        JButton b = new JButton();
        b.setName(String.format("%d", n));
        b.addChangeListener(this::kbPressed);
        b.setPreferredSize(new Dimension(16, 32));
        bc.gridx = x;
        bc.gridy = y;
        kb.add(b, bc);
    }



    void deduce() {
        stateA1 = getCurrentPreset().a1 % 4;
        litPadA1 = getCurrentPreset().a1 / 4;
        stateA0 = getCurrentPreset().a0 % 4;
        litPadA0 = getCurrentPreset().a0 / 4;
        if (getCurrentPreset().b1 == 32) {
            litPadB1 = litPadA1;
            stateB1 = 4;
        } else {
            stateB1 = getCurrentPreset().b1 % 4;
            litPadB1 = getCurrentPreset().b1 / 4;
        }
        if (getCurrentPreset().b0 == 32) {
            litPadB0 = litPadA0;
            stateB0 = 4;
        } else {
            stateB0 = getCurrentPreset().b0 % 4;
            litPadB0 = getCurrentPreset().b0 / 4;
        }
        litPadLFF = getCurrentPreset().mFunc;
    }

/*    void presetSelected(ListSelectionEvent e) {
        numCurrentPreset = jlPresets.getSelectedIndex();
        sendPreset(getCurrentPreset());
        updateGUIfromPreset();
    }
*/
    void midiInSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        System.out.println("Midi in selected");

        if (trMidiIn != null) trMidiIn.close();
        if (mdMidiIn != null) mdMidiIn.close();
        trMidiIn = null;
        mdMidiIn = null;
        int sel = jlMidiIns.getSelectedIndex();
        if (sel >= midiDeviceTransmitters.size() || sel < 0) {
            scanMidiInDevices();
            return;
        }
        mdMidiIn = midiDeviceTransmitters.get(sel);
        if (mdMidiIn == null)
            return;
        try {
            mdMidiIn.open();
            trMidiIn = mdMidiIn.getTransmitter();
            trMidiIn.setReceiver(this);
            System.out.println("got the transmitter");
        } catch (MidiUnavailableException er) {
            scanMidiInDevices();
            // throw new RuntimeException(e);
        }
    }

    void midiOutSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        System.out.println("Midi out selected");

        if (rcMidiOut != null) rcMidiOut.close();
        if (mdMidiOut != null) mdMidiOut.close();
        rcMidiOut = null;
        mdMidiOut = null;
        int sel = jlMidiOuts.getSelectedIndex();
        if (sel >= midiDeviceReceivers.size() || sel < 0) {
            scanMidiOutDevices();
            return;
        }
        mdMidiOut = midiDeviceReceivers.get(sel);
        if (mdMidiOut == null)
            return;
        try {
            mdMidiOut.open();
            rcMidiOut = mdMidiOut.getReceiver();
            System.out.println("receiver got");
        } catch (MidiUnavailableException er) {
            scanMidiOutDevices();
            // throw new RuntimeException(e);
        }
    }

    void sendPreset(Preset p) {
        sendCC(74, p.fCut);
        sendCC(73, p.fMod);
        sendCC(72, p.fEnv);
        sendCC(71, p.fRes);
        sendCC(16, p.vEnv);

        sendCC(1, p.mrph);
        sendCC(18, p.mMod);
        sendCC(19, p.mRat);

        sendCC(78, p.a1);
        sendCC(79, p.b1);
        sendCC(77, p.mFunc);
        sendCC(75, p.a0);
        sendCC(76, p.b0);

        sendCC(94, p.sym);
        sendCC(91, p.rev);
        sendCC(93, p.ch);
    }
/*
    void loadClicked(ActionEvent e) {
        JFileChooser fc = new JFileChooser("d:", FileSystemView.getFileSystemView());
        fc.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV preset file", "csv");
        fc.addChoosableFileFilter(filter);
        int res = fc.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION)
            return;
        System.out.println(fc.getSelectedFile().getName());
        System.out.println(fc.getSelectedFile().getAbsolutePath());

    }
*/
    void getFromTeensy(ActionEvent e) {
        sendPC((Integer) spGetFromTeensy.getValue());
        sendCC(118, 0);

    }

    void saveToTeensy(ActionEvent e) {
        int v = (Integer) spSaveToTeensy.getValue();

        sendCC(119, v);
    }

    void padClicked(ActionEvent e) {
        deduce();
        JButton b = (JButton) e.getSource();
        System.out.println(b.getName());
        int numPad = Integer.parseInt(b.getName());
        if (numPad < 8) {
            if (numPad == litPadA1) {
                stateA1 = (stateA1 + 1) % 4;
            } else {
                litPadA1 = numPad;
            }
            getCurrentPreset().a1 = litPadA1 * 4 + stateA1;
            sendCC(78, getCurrentPreset().a1);
        } else if (numPad < 16) {
            int n = numPad - 8;
            if (n == litPadB1) {
                stateB1 = (stateB1 + 1) % 5;
            } else {
                litPadB1 = n;
            }
            if (stateB1 == 4)
                getCurrentPreset().b1 = 32;
            else
                getCurrentPreset().b1 = litPadB1 * 4 + stateB1;
            sendCC(79, getCurrentPreset().b1);
        } else if (numPad < 24) {
            litPadLFF = numPad - 16;
            getCurrentPreset().mFunc = litPadLFF;
            sendCC(77, getCurrentPreset().mFunc);
        } else if (numPad < 32) {
            int n = numPad - 24;
            if (n == litPadA0) {
                stateA0 = (stateA0 + 1) % 4;
            } else {
                litPadA0 = n;
            }
            getCurrentPreset().a0 = litPadA0 * 4 + stateA0;
            sendCC(75, getCurrentPreset().a0);
        } else if (numPad < 40) {
            int n = numPad - 32;
            if (n == litPadB0) {
                stateB0 = (stateB0 + 1) % 5;
            } else {
                litPadB0 = n;
            }
            if (stateB0 == 4)
                getCurrentPreset().b0 = 32;
            else
                getCurrentPreset().b0 = litPadB0 * 4 + stateB0;
            sendCC(76, getCurrentPreset().b0);
        }

        updatePadColors();

    }

    void revClicked(ActionEvent e) {
        JButton b = (JButton) e.getSource();
        System.out.println(b.getName());
        getCurrentPreset().rev = Integer.parseInt(b.getName());
        sendCC(91, getCurrentPreset().rev);
        updateRevColors();
        System.out.println(Preset.csvTitles());
        System.out.println(getCurrentPreset().toCsv());
    }

    void resoClicked(ActionEvent e) {
        JButton b = (JButton) e.getSource();
        System.out.println(b.getName());
        getCurrentPreset().fRes = 5 - Integer.parseInt(b.getName());
        sendCC(71, getCurrentPreset().fRes);
        updateResoColors();
    }

    void cutoffChanged(ChangeEvent e) {
        getCurrentPreset().fCut = sFCut.getValue();
        sendCC(74, getCurrentPreset().fCut);
    }

    void fModChanged(ChangeEvent e) {
        getCurrentPreset().fMod = sFMod.getValue() + 64;
        sendCC(73, getCurrentPreset().fMod);
    }

    void fEnvChanged(ChangeEvent e) {
        getCurrentPreset().fEnv = sFEnv.getValue();
        sendCC(72, getCurrentPreset().fEnv);
    }

    void mrphChanged(ChangeEvent e) {
        getCurrentPreset().mrph = sMrph.getValue();
        sendCC(1, getCurrentPreset().mrph);
    }

    void mModChanged(ChangeEvent e) {
        getCurrentPreset().mMod = sMMod.getValue();
        sendCC(18, getCurrentPreset().mMod);
    }

    void mRateChanged(ChangeEvent e) {
        getCurrentPreset().mRat = sMRat.getValue();
        sendCC(19, getCurrentPreset().mRat);
    }

    void vEnvChanged(ChangeEvent e) {
        getCurrentPreset().vEnv = sVEnv.getValue();
        sendCC(16, getCurrentPreset().vEnv);
    }

    void volChanged(ChangeEvent e) {
        sendCC(7, sVol.getValue());
    }

    void chorusSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        getCurrentPreset().ch = jlChorus.getSelectedIndex();
        sendCC(93, getCurrentPreset().ch);
    }

    void symSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        getCurrentPreset().sym = jlSym.getSelectedIndex();
        sendCC(94, getCurrentPreset().sym);
    }

    boolean[] kbKeys = new boolean[7*12];

    void kbPressed(ChangeEvent e) {
        JButton b = (JButton) e.getSource();
        ButtonModel model = b.getModel();
        int note = Integer.parseInt(b.getName());
        if (model.isPressed() && !kbKeys[note]) {
            System.out.println(b.getName() + " pressed -> note on");
            kbKeys[note] = true;
            sendNoteOn(24+note,127);

        }
        if (!model.isPressed() && kbKeys[note]) {
            System.out.println(b.getName() + " released -> note off");
            kbKeys[note] = false;
            sendNoteOn(24+note,0);
        }

    }

    private void printDeviceType(MidiDevice device) {
        if (device instanceof Sequencer) {
            System.out.println("This is a sequencer");
        } else if (device instanceof Synthesizer) {
            System.out.println("This is a synthesizer");
        } else {
            System.out.print("This is a MIDI port ");
            if (device.getMaxReceivers() != 0) {
                System.out.print("IN ");
            }
            if (device.getMaxTransmitters() != 0) {
                System.out.print("OUT ");
            }
            System.out.println();
        }
    }

    void updateGUIfromPreset() {
        updatePadColors();
        updateRevColors();
        updateResoColors();

        jlSym.setSelectedIndex(getCurrentPreset().sym);
        jlChorus.setSelectedIndex(getCurrentPreset().ch);
        sFCut.setValue(getCurrentPreset().fCut);
        sFMod.setValue(getCurrentPreset().fMod - 64);
        sFEnv.setValue(getCurrentPreset().fEnv);
        sMrph.setValue(getCurrentPreset().mrph);
        sMMod.setValue(getCurrentPreset().mMod);
        sMRat.setValue(getCurrentPreset().mRat);
        sVEnv.setValue(getCurrentPreset().vEnv);
    }

    private String maxToString(int max) {
        return max == -1 ? "Unlimited" : String.valueOf(max);
    }

    // Main driver method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(TRIEditor::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("TR-I Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = new TRIEditor();

        newContentPane.setPreferredSize(new Dimension(900,800));
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

}
