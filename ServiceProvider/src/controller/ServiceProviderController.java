package controller;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.bouncycastle.openssl.PEMReader;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import ssl.ServiceProviderServer;

public class ServiceProviderController {
	ServiceProviderServer sps;

	@FXML
	private CheckBox adressCheck;

	@FXML
	private CheckBox fotoCheck;

	@FXML
	private TextArea communicationArea;

	@FXML
	private HBox submitButton;

	@FXML
	private ComboBox<String> providerCombo;

	@FXML
	private CheckBox nameCheck;

	@FXML
	private CheckBox ageCheck;

	@FXML
	private CheckBox countryCheck;

	@FXML
	private CheckBox genderCheck;

	@FXML
	private CheckBox birthDateCheck;

	private Thread serverThread = null;

	@FXML
	void lockProvider(ActionEvent event) {
		String output = providerCombo.getSelectionModel().getSelectedItem().toString();
		try {
			if (serverThread == null) {
				sps = new ServiceProviderServer(getCertificate(output), getKey(output), this);

				Thread thread = new Thread(sps);
				thread.start();

				this.setServerThread(thread);
			} else {
				sps.setSpPrivateKey(getKey(output));
				sps.setX509Certificate(getCertificate(output));
				sps.restart();
			}
		} catch (CertificateException | FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	void submitSettings(ActionEvent event) {
		boolean name = nameCheck.selectedProperty().getValue();
		boolean adress = adressCheck.selectedProperty().getValue();
		boolean foto = fotoCheck.selectedProperty().getValue();
		boolean age = ageCheck.selectedProperty().getValue();
		boolean country = countryCheck.selectedProperty().getValue();
		boolean gender = genderCheck.selectedProperty().getValue();
		boolean birthday = birthDateCheck.selectedProperty().getValue();
		String output = providerCombo.getSelectionModel().getSelectedItem().toString();

		try {
			X509Certificate cert = getCertificate(output);
		} catch (Exception e) {
			e.printStackTrace();
		}

		String submit = "Selected: " + name + "," + adress + "," + foto + "," + age + "," + country + "," + birthday
				+ " for " + output;
		//addText(submit);

		byte[] query = new byte[7];
		query[0] = (name) ? (byte) 1 : (byte) 0;
		query[1] = (adress) ? (byte) 1 : (byte) 0;
		query[2] = (foto) ? (byte) 1 : (byte) 0;
		query[3] = (age) ? (byte) 1 : (byte) 0;
		query[4] = (country) ? (byte) 1 : (byte) 0;
		query[5] = (gender) ? (byte) 1 : (byte) 0;
		query[6] = (birthday) ? (byte) 1 : (byte) 0;
		sps.releaseAttributes(query);
	}

	private RSAPrivateKey getKey(String output) throws IOException {
		String fileName = null;
		switch (output) {
		case "Overheid 1":
			fileName = "../Certificaten2/gov1.key";
			break;
		case "Overheid 2":
			fileName = "../Certificaten2/gov2.key";
			break;
		case "Sociaal Netwerk 1":
			fileName = "../Certificaten2/soc1.key";
			break;
		case "Sociaal Netwerk 2":
			fileName = "../Certificaten2/soc2.key";
			break;
		case "Default 1":
			fileName = "../Certificaten2/def1.key";
			break;
		case "Default 2":
			fileName = "../Certificaten2/def2.key";
			break;
		case "Keuze 1":
			fileName = "../Certificaten2/oth1.key";
			break;
		case "Keuze 2":
			fileName = "../Certificaten2/oth2.key";
			break;
		default:
			break;
		}
		FileReader fr = null, fr2 = null;
		fr2 = new FileReader(fileName);
		PEMReader pemReader = new PEMReader(fr2);
		KeyPair kp = (KeyPair) pemReader.readObject();
		RSAPrivateKey sk = (RSAPrivateKey) kp.getPrivate();
		return sk;
	}

	private X509Certificate getCertificate(String output) throws CertificateException, FileNotFoundException {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		FileReader fr = null;
		String fileName = new String();
		// Alle paswoorden zijn gelijk aan password
		switch (output) {
		case "Overheid 1":
			addText("SP \n\t Overheid 1 werd geselecteerd \n\t Certificaat wordt opgehaald");
			fileName = "../Certificaten2/gov1.crt";
			break;
		case "Overheid 2":
			addText("SP \n\t Overheid 2 werd geselecteerd \n\t Certificaat wordt opgehaald");
			fileName = "../Certificaten2/gov2.crt";
			break;
		case "Sociaal Netwerk 1":
			addText("SP \n\t Sociaal Netwerk 1 werd geselecteerd \n\t Certificaat wordt opgehaald");
			fileName = "../Certificaten2/soc1.crt";
			break;
		case "Sociaal Netwerk 2":
			addText("SP \n\t Sociaal Netwerk 1 werd geselecteerd \n\t Certificaat wordt opgehaald");
			fileName = "../Certificaten2/soc2.crt";
			break;
		case "Default 1":
			addText("SP \n\t Default 1 werd geselecteerd \n\t Certificaat wordt opgehaald");
			fileName = "../Certificaten2/def1.crt";
			break;
		case "Default 2":
			addText("SP \n\t Default 1 werd geselecteerd \n\t Certificaat wordt opgehaald");
			fileName = "../Certificaten2/def2.crt";
			break;
		case "Keuze 1":
			addText("SP \n\t Keuze 1 werd geselecteerd \n\t Certificaat wordt opgehaald");
			fileName = "../Certificaten2/oth1.crt";
			break;
		case "Keuze 2":
			addText("SP \n\t Keuze 1 werd geselecteerd \n\t Certificaat wordt opgehaald");
			fileName = "../Certificaten2/oth2.crt";
			break;
		default:
			break;
		}
		fr = new FileReader(fileName);
		PEMReader pemReader = new PEMReader(fr);
		X509Certificate cert = null;
		try {
			cert = (X509Certificate) pemReader.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cert;
	}

	@FXML
	public void initialize() {
		providerCombo.getItems().addAll("Overheid 1", "Overheid 2", "Sociaal Netwerk 1", "Sociaal Netwerk 2",
				"Default 1", "Default 2", "Keuze 1", "Keuze 2");
		providerCombo.getSelectionModel().selectFirst();

	}

	public void addText(String text) {
		communicationArea.appendText(text + "\n");
	}

	public Thread getServerThread() {
		return serverThread;
	}

	public void setServerThread(Thread serverThread) {
		this.serverThread = serverThread;
	}
}
