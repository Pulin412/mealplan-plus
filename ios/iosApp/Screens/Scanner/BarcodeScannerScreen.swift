import SwiftUI
import AVFoundation
import Vision

struct BarcodeScannerScreen: View {
    @Environment(\.dismiss) var dismiss
    @StateObject private var viewModel = BarcodeScannerViewModel()
    var onBarcodeScanned: (String) -> Void

    var body: some View {
        ZStack {
            // Camera preview
            CameraPreview(session: viewModel.session)
                .ignoresSafeArea()

            // Overlay
            VStack {
                // Top bar
                HStack {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title)
                            .foregroundColor(.white)
                    }
                    Spacer()
                    Button(action: { viewModel.toggleFlash() }) {
                        Image(systemName: viewModel.isFlashOn ? "bolt.fill" : "bolt.slash.fill")
                            .font(.title2)
                            .foregroundColor(.white)
                    }
                }
                .padding()

                Spacer()

                // Scan area indicator
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.green, lineWidth: 3)
                    .frame(width: 280, height: 180)
                    .overlay(
                        VStack {
                            Image(systemName: "barcode.viewfinder")
                                .font(.system(size: 40))
                                .foregroundColor(.green)
                            Text("Point camera at barcode")
                                .font(.caption)
                                .foregroundColor(.white)
                        }
                    )

                Spacer()

                // Status
                if let barcode = viewModel.scannedBarcode {
                    VStack(spacing: 12) {
                        Text("Barcode Found!")
                            .font(.headline)
                            .foregroundColor(.white)
                        Text(barcode)
                            .font(.title3)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 10)
                            .background(Color.black.opacity(0.7))
                            .cornerRadius(8)

                        Button(action: {
                            onBarcodeScanned(barcode)
                            dismiss()
                        }) {
                            Text("Use This Barcode")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.green)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                        }
                        .padding(.horizontal, 40)
                    }
                    .padding()
                    .background(Color.black.opacity(0.5))
                }

                // Instructions
                if viewModel.scannedBarcode == nil {
                    Text("Scanning for barcodes...")
                        .font(.subheadline)
                        .foregroundColor(.white)
                        .padding()
                        .background(Color.black.opacity(0.5))
                        .cornerRadius(8)
                        .padding(.bottom, 40)
                }
            }

            // Permission denied overlay
            if !viewModel.cameraPermissionGranted {
                VStack(spacing: 20) {
                    Image(systemName: "camera.fill")
                        .font(.system(size: 60))
                        .foregroundColor(.gray)
                    Text("Camera Access Required")
                        .font(.headline)
                    Text("Please enable camera access in Settings to scan barcodes")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                    Button("Open Settings") {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                    .padding()
                    .background(Color.green)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                .padding()
                .background(Color.white)
                .cornerRadius(20)
                .padding(40)
            }
        }
        .onAppear {
            viewModel.checkPermissions()
        }
        .onDisappear {
            viewModel.stopSession()
        }
    }
}

class BarcodeScannerViewModel: NSObject, ObservableObject {
    @Published var scannedBarcode: String?
    @Published var isFlashOn = false
    @Published var cameraPermissionGranted = true

    let session = AVCaptureSession()
    private var videoOutput: AVCaptureVideoDataOutput?
    private let sessionQueue = DispatchQueue(label: "barcode.session")

    override init() {
        super.init()
    }

    func checkPermissions() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            cameraPermissionGranted = true
            setupSession()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                DispatchQueue.main.async {
                    self?.cameraPermissionGranted = granted
                    if granted {
                        self?.setupSession()
                    }
                }
            }
        default:
            cameraPermissionGranted = false
        }
    }

    private func setupSession() {
        sessionQueue.async { [weak self] in
            guard let self = self else { return }

            self.session.beginConfiguration()
            self.session.sessionPreset = .high

            // Add video input
            guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
                  let input = try? AVCaptureDeviceInput(device: device) else {
                return
            }

            if self.session.canAddInput(input) {
                self.session.addInput(input)
            }

            // Add video output for Vision
            let output = AVCaptureVideoDataOutput()
            output.setSampleBufferDelegate(self, queue: DispatchQueue(label: "barcode.output"))

            if self.session.canAddOutput(output) {
                self.session.addOutput(output)
            }

            self.videoOutput = output
            self.session.commitConfiguration()
            self.session.startRunning()
        }
    }

    func stopSession() {
        sessionQueue.async { [weak self] in
            self?.session.stopRunning()
        }
    }

    func toggleFlash() {
        guard let device = AVCaptureDevice.default(for: .video),
              device.hasTorch else { return }

        do {
            try device.lockForConfiguration()
            device.torchMode = isFlashOn ? .off : .on
            isFlashOn.toggle()
            device.unlockForConfiguration()
        } catch {
            print("Flash error: \(error)")
        }
    }
}

extension BarcodeScannerViewModel: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        guard scannedBarcode == nil,
              let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }

        let request = VNDetectBarcodesRequest { [weak self] request, error in
            guard let results = request.results as? [VNBarcodeObservation],
                  let barcode = results.first?.payloadStringValue else { return }

            DispatchQueue.main.async {
                self?.scannedBarcode = barcode
                // Vibrate on success
                let generator = UINotificationFeedbackGenerator()
                generator.notificationOccurred(.success)
            }
        }

        request.symbologies = [.ean8, .ean13, .upce, .code128, .code39, .code93, .qr]

        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, options: [:])
        try? handler.perform([request])
    }
}

struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession

    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)

        let previewLayer = AVCaptureVideoPreviewLayer(session: session)
        previewLayer.videoGravity = .resizeAspectFill
        previewLayer.frame = view.bounds
        view.layer.addSublayer(previewLayer)

        context.coordinator.previewLayer = previewLayer
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        context.coordinator.previewLayer?.frame = uiView.bounds
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    class Coordinator {
        var previewLayer: AVCaptureVideoPreviewLayer?
    }
}

struct BarcodeScannerScreen_Previews: PreviewProvider {
    static var previews: some View {
        BarcodeScannerScreen { _ in }
    }
}
