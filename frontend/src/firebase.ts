import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";

const firebaseConfig = {
  apiKey: "AIzaSyDtbjFO_F7l9kkoj0WTtvBw8v-gycbZzhg",
  authDomain: "send-financial-app-2026.firebaseapp.com",
  projectId: "send-financial-app-2026",
  storageBucket: "send-financial-app-2026.firebasestorage.app",
  messagingSenderId: "396469715034",
  appId: "1:396469715034:web:bc7875d2337727d33f5863"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize Services
export const auth = getAuth(app);
export const db = getFirestore(app);

export default app;
