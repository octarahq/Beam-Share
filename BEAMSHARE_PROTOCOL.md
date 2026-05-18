# Protocol Documentation: BeamShare

BeamShare is a decentralized file transfer protocol using mDNS for discovery and ECDSA/AES-GCM for secure communication.

## 1. Discovery (mDNS)
BeamShare services announce themselves via DNS-SD (mDNS).

- **Service Type**: `_beamshare._tcp`
- **Service Name**: User-defined device name.
- **Port**: Dynamically assigned (0-65535).
- **TXT Records**:
    - `model`: Device model (e.g., "Pixel 7").
    - `device_type`: Typically "smartphone".
    - `node_id`: Public Key of the device (Hex format), used for unique identification and encryption.

## 2. Communication Channel
Communication happens over a raw TCP Socket.

## 3. Handshake & Authentication

### Step 1: Metadata (Sender -> Receiver)
The sender connects and sends a single JSON line containing file information.

```json
{
  "name": "image.png",
  "size": 123456,
  "type": "image/png",
  "sender_id": "<PUBLIC_KEY_HEX>",
  "sender_name": "My Phone",
  "encryption_supported": true
}
```

### Step 2: Challenge (Receiver -> Sender)
The receiver generates a 32-byte random nonce (hex encoded) and sends it.

```text
<RANDOM_NONCE_HEX>
```

### Step 3: Proof of Identity (Sender -> Receiver)
The sender signs the nonce using their private key (ECDSA with SHA-256) and sends the signature.

```text
<SIGNATURE_HEX>
```

The receiver verifies the signature using the `sender_id` (Public Key) provided in Step 1.

### Step 4: User Decision
The receiver prompts the user.
- If accepted: Receiver sends `OK\n`.
- If declined: Receiver sends `CANCEL\n` and closes the socket.

## 4. Encrypted Data Transmission

If `encryption_supported` is true, both parties derive a shared secret using **ECDH** (Elliptic Curve Diffie-Hellman) based on their private key and the other party's public key.

1. **Shared Secret**: Derived using `secp256r1` curve.
2. **Key Derivation**: The shared secret is hashed using SHA-256 to produce a 256-bit AES key.
3. **Encryption**: AES-256-GCM.
    - The sender generates a 12-byte random **IV**.
    - The sender writes the 12-byte **IV** to the stream first.
    - The rest of the stream is encrypted with AES-GCM (128-bit authentication tag).
    - The tag is automatically appended to the end of the stream when the cipher stream is closed.

## 5. Status Updates
- **Progress**: Calculated as `(bytes_received / total_size)`.
- **Completion**: The receiver verifies the GCM Authentication Tag. If it fails (AEADBadTagException), the file is corrupted/tampered and should be deleted.
- **History**: Both parties log the event with a unique UUID.
