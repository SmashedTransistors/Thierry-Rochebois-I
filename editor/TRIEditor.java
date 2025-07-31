import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

public class TRIEditor extends JPanel {

    public static final Color REV_OFF = new Color(200, 160, 160);
    public static final Color REV_ON = Color.red;
    public static final Color FUNC_OFF = new Color(180, 170, 230);
    public static final Color FUNC_ON = new Color(220, 90, 255);

    static class Preset {
        String name="tagada";
        int fCut=127, fMod = 127, fEnv=127, fRes=0,
                vEnv, mrph, mMod, mRat, a1, b1, mFunc, a0, b0, sym, rev, ch;
        public String toCsv() {
            return  String.format("%12s,",name )
                    +String.format("  %3d,  %3d,  %3d,  %3d,  %3d,",fCut, fMod, fEnv, fRes, vEnv)
                    +String.format("  %3d,  %3d,  %3d,",mrph, mMod, mRat)
                    +String.format(" %2d, %2d,   %3d, %2d, %2d,",a1, b1, mFunc, a0, b0)
                    +String.format(" %3d, %3d, %2d",sym, rev, ch);
        }
        public static String csvTitles() {
            return "Preset name " +
                    ", fCut, fMod, fEnv, fRes, vEnv" +
                    ", mrph, mMod, mRat" +
                    ", a1, b1, mFunc, a0, b0" +
                    ", sym" + ", rev" + ", ch";
        }
    }
    Preset currentPreset = new Preset();

    //active pads
    int litPadA0, stateA0;
    int litPadA1, stateA1;
    int litPadB0, stateB0;
    int litPadB1, stateB1;
    int litPadLFF;

    DefaultListModel<String> listModelMidiOut = new DefaultListModel<>();
    JList<String> jlMidiOuts;

    JButton[] bPads = new JButton[40];
    JButton[] bRev = new JButton[8];
    JButton[] bReso = new JButton[6];
    JSlider sCutoff = new JSlider(JSlider.VERTICAL, 0, 127, 127);
    JSlider sFMod = new JSlider(JSlider.VERTICAL, -64, 63, 63);
    JSlider sFEnv = new JSlider(JSlider.VERTICAL, 0, 127, 127);

    JSlider sMrph = new JSlider(JSlider.VERTICAL, 0, 127, 0);
    JSlider sMMod = new JSlider(JSlider.VERTICAL, 0, 127, 0);
    JSlider sMRate = new JSlider(JSlider.VERTICAL, 0, 127, 0);

    JSlider sVEnv = new JSlider(JSlider.VERTICAL, 0, 127, 0);
    JSlider sVol = new JSlider(JSlider.VERTICAL, 0, 127, 127);

    DefaultListModel<String> listModelChorus = new DefaultListModel<>();
    JList<String> jlChorus;

    DefaultListModel<String> listModelSym = new DefaultListModel<>();
    JList<String> jlSym;

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
            if (numRev != currentPreset.rev) {
                bRev[numRev].setBackground(REV_OFF);
            } else {
                bRev[numRev].setBackground(REV_ON);
            }
        }
    }

    private void updateResoColors() {
        Color resoOff = new Color(200, 230, 200);
        for (int numReso = 0; numReso < 6; numReso++) {
            if (numReso != 5 - currentPreset.fRes) {
                bReso[numReso].setBackground(resoOff);
            } else {
                bReso[numReso].setBackground(Color.green);
            }
        }
    }

    ArrayList<String> midiDeviceNames;
    ArrayList<MidiDevice> midiDevices;

    void sendCC(int cc, int val) {
        int sel = jlMidiOuts.getSelectedIndex();
        if (sel >= midiDevices.size() || sel < 0) {
            scanMidiDevices();
            return;
        }
        MidiDevice md = midiDevices.get(sel);
        try {
            md.open();
            Receiver receiver = md.getReceiver();
            ShortMessage m = new ShortMessage();
            m.setMessage(ShortMessage.CONTROL_CHANGE, 0, cc, val);
            receiver.send(m, -1);
            receiver.close();
            md.close();
        } catch (MidiUnavailableException e) {
            scanMidiDevices();
            // throw new RuntimeException(e);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    void scanMidiDevices() {
        midiDeviceNames = new ArrayList<>();
        midiDevices = new ArrayList<>();
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
                    midiDeviceNames.add(info.getName());
                    midiDevices.add(device);
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
        for (String n : midiDeviceNames) {
            listModelMidiOut.addElement(n);
        }


    }

    void autoSelectTeensy() {
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

        scanMidiDevices();
        //Create the list and put it in a scroll pane.
        jlMidiOuts = new JList<>(listModelMidiOut);
        jlMidiOuts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jlMidiOuts.setSelectedIndex(0);
        jlMidiOuts.setVisibleRowCount(10);
        this.add(new JScrollPane(jlMidiOuts));
        autoSelectTeensy();


        JPanel grPads = new JPanel();
        GridLayout glPads = new GridLayout(7, 10, 5, 5);
        grPads.setLayout(glPads);
        String[] colNames = {"", "SAW", "STR", "SYN", "PLK", "DXW", "TOY", "GRG", "END", "Reso"};
        for (int col = 0; col < 10; col++) {

            JLabel l = new JLabel(colNames[col], JLabel.CENTER);
            grPads.add(l);
        }
        String[] rowNames = {"Morph pr.a1", "Morph pr.b1", "Func", "Morph pr.a0", "Morph pr.b0"};
        String[] funcNames = {"Decay", "LFO", "Dec+LFO", "Dec+fLFO", "comLFO", "LFOs", "velDec", "rand"};
        for (int row = 0; row < 5; row++) {

            JLabel l = new JLabel(rowNames[row]);
            grPads.add(l);
            for (int col = 0; col < 8; col++) {
                int numPad = row * 8 + col;
                if (row != 2) {
                    bPads[numPad] = new JButton();
                } else {
                    bPads[numPad] = new JButton(funcNames[col]);
                    bPads[numPad].setMargin(new Insets(0, 0, 0, 0));
                }
                bPads[numPad].setPreferredSize(new Dimension(70, 35));
                bPads[numPad].setName(String.format("%d", numPad));
                // bPads[i].setBackground(c[i/8]);
                bPads[numPad].addActionListener(this::padClicked);
                grPads.add(bPads[numPad]);
            }
            //reso
            bReso[row] = new JButton();
            bReso[row].setPreferredSize(new Dimension(50, 25));
            bReso[row].setName(String.format("%d", row));
            // bPads[i].setBackground(c[i/8]);
            bReso[row].addActionListener(this::resoClicked);
            grPads.add(bReso[row]);
        }
        //reverb buttons
        {
            JLabel l = new JLabel("Reverb");
            grPads.add(l);
            for (int col = 0; col < 8; col++) {

                bRev[col] = new JButton();
                bRev[col].setPreferredSize(new Dimension(20, 20));
                bRev[col].setMargin(new Insets(5, 5, 5, 5));
                bRev[col].setName(String.format("%d", col));
                // bPads[i].setBackground(c[i/8]);
                bRev[col].addActionListener(this::revClicked);
                grPads.add(bRev[col]);
            }
        }
        bReso[5] = new JButton("");
        bReso[5].setPreferredSize(new Dimension(50, 25));
        bReso[5].setName(String.format("%d", 5));
        // bPads[i].setBackground(c[i/8]);
        bReso[5].addActionListener(this::resoClicked);
        grPads.add(bReso[5]);

        updatePadColors();
        updateRevColors();
        updateResoColors();
        this.add(grPads);


        sCutoff.addChangeListener(this::cutoffChanged);
        this.add(slPanel(sCutoff, "FCut", Color.red));
        sFMod.addChangeListener(this::fModChanged);
        this.add(slPanel(sFMod, "FMod", Color.red));
        sFEnv.addChangeListener(this::fEnvChanged);
        this.add(slPanel(sFEnv, "FEnv", Color.red));

        sMrph.addChangeListener(this::mrphChanged);
        this.add(slPanel(sMrph, "Mrph", Color.orange.darker()));
        sMMod.addChangeListener(this::mModChanged);
        this.add(slPanel(sMMod, "MMod", Color.orange.darker()));
        sMRate.addChangeListener(this::mRateChanged);
        this.add(slPanel(sMRate, "MRate", Color.orange.darker()));

        sVEnv.addChangeListener(this::vEnvChanged);
        this.add(slPanel(sVEnv, "VEnv", Color.black));
        sVol.addChangeListener(this::volChanged);
        this.add(slPanel(sVol, "Vol", Color.black));

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


        this.add(jlChorus);
        this.add(jlSym);


        this.setVisible(true);
    }

    void deduce() {
        stateA1 = currentPreset.a1 % 4;
        litPadA1 = currentPreset.a1 / 4;
        stateA0 = currentPreset.a0 % 4;
        litPadA0 = currentPreset.a0 / 4;
        if (currentPreset.b1 == 32) {
            litPadB1 = litPadA1;
            stateB1 = 4;
        } else {
            stateB1 = currentPreset.b1 % 4;
            litPadB1 = currentPreset.b1 / 4;
        }
        if (currentPreset.b0 == 32) {
            litPadB0 = litPadA0;
            stateB0 = 4;
        } else {
            stateB0 = currentPreset.b0 % 4;
            litPadB0 = currentPreset.b0 / 4;
        }
    }

    void sendPreset(Preset p){
        sendCC(74, p.fCut);
        sendCC(73, p.fMod);
        sendCC(72, p.fEnv);
        sendCC(71, p.fRes);
        sendCC(16, p.vEnv);

        sendCC(1,  p.mrph);
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
            currentPreset.a1 = litPadA1 * 4 + stateA1;
            sendCC(78, currentPreset.a1);
        } else if (numPad < 16) {
            int n = numPad - 8;
            if (n == litPadB1) {
                stateB1 = (stateB1 + 1) % 5;
            } else {
                litPadB1 = n;
            }
            if (stateB1 == 4)
                currentPreset.b1 = 32;
            else
                currentPreset.b1 = litPadB1 * 4 + stateB1;
            sendCC(79, currentPreset.b1);
        } else if (numPad < 24) {
            litPadLFF = numPad - 16;
            currentPreset.mFunc = litPadLFF;
            sendCC(77, currentPreset.mFunc);
        } else if (numPad < 32) {
            int n = numPad - 24;
            if (n == litPadA0) {
                stateA0 = (stateA0 + 1) % 4;
            } else {
                litPadA0 = n;
            }
            currentPreset.a0 = litPadA0 * 4 + stateA0;
            sendCC(75, currentPreset.a0);
        } else if (numPad < 40) {
            int n = numPad - 32;
            if (n == litPadB0) {
                stateB0 = (stateB0 + 1) % 5;
            } else {
                litPadB0 = n;
            }
            if (stateB0 == 4)
                currentPreset.b0 = 32;
            else
                currentPreset.b0 = litPadB0 * 4 + stateB0;
            sendCC(76, currentPreset.b0);
        }

        updatePadColors();

    }

    void revClicked(ActionEvent e) {
        JButton b = (JButton) e.getSource();
        System.out.println(b.getName());
        currentPreset.rev = Integer.parseInt(b.getName());
        sendCC(91, currentPreset.rev);
        updateRevColors();
        System.out.println(Preset.csvTitles());
        System.out.println(currentPreset.toCsv());
    }

    void resoClicked(ActionEvent e) {
        JButton b = (JButton) e.getSource();
        System.out.println(b.getName());
        currentPreset.fRes = 5 - Integer.parseInt(b.getName());
        sendCC(71, currentPreset.fRes);
        updateResoColors();
    }

    void cutoffChanged(ChangeEvent e) {
        currentPreset.fCut = sCutoff.getValue();
        sendCC(74, currentPreset.fCut);
    }

    void fModChanged(ChangeEvent e) {
        currentPreset.fMod = sFMod.getValue() + 64;
        sendCC(73, currentPreset.fMod);
    }

    void fEnvChanged(ChangeEvent e) {
        currentPreset.fEnv = sFEnv.getValue();
        sendCC(72, currentPreset.fEnv);
    }

    void mrphChanged(ChangeEvent e) {
        currentPreset.mrph = sMrph.getValue();
        sendCC(1, currentPreset.mrph);
    }

    void mModChanged(ChangeEvent e) {
        currentPreset.mMod = sMMod.getValue();
        sendCC(18, currentPreset.mMod);
    }

    void mRateChanged(ChangeEvent e) {
        currentPreset.mRat = sMRate.getValue();
        sendCC(19, currentPreset.mRat);
    }

    void vEnvChanged(ChangeEvent e) {
        currentPreset.vEnv = sVEnv.getValue();
        sendCC(16, currentPreset.vEnv);
    }

    void volChanged(ChangeEvent e) {
        sendCC(7, sVol.getValue());
    }

    void chorusSelected(ListSelectionEvent e) {
        currentPreset.ch = jlChorus.getSelectedIndex();
        sendCC(93, currentPreset.ch);
    }

    void symSelected(ListSelectionEvent ignoredE) {
        currentPreset.sym = jlSym.getSelectedIndex();
        sendCC(94, currentPreset.sym);
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

    private String maxToString(int max) {
        return max == -1 ? "Unlimited" : String.valueOf(max);
    }

    // Main driver method
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(TRIEditor::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("TR-I Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = new TRIEditor();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

}
