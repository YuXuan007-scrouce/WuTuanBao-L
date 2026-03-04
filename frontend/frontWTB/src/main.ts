import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import {createPinia} from "pinia";
import router from './router';
import Vant from "vant";
import "vant/lib/index.css"; // ❗ 必须


const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(Vant);

app.mount('#app')
