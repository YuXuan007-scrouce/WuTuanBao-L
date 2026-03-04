<template>
  <div class="login-page">
    <!-- 标题 -->
   <div class="h-[30vh] flex flex-col justify-center items-center">
      <van-image round width="30vw" height="30vw" :src="defaultAvatarUrl" />
    </div>

    <van-form @submit="onSubmit">
      <!-- 手机号 -->
      <van-field
        v-model="loginInfo.phone"
        label="手机号"
        placeholder="请输入手机号"
        type="tel"
        maxlength="11"
        clearable
        :rules="mobileRules"
      />

      <!-- 密码 -->
      <!-- <van-field
        v-model="loginInfo.password"
        label="密码"
        type="password"
        placeholder="请输入密码"
        clearable
        :rules="passwordRules"
      /> -->

      <!-- 验证码 -->
      <van-field
        v-model="loginInfo.code"
        label="验证码"
        placeholder="请输入验证码"
        clearable
        :rules="codeRules"
      >
        <template #button>
          <van-button
            size="small"
            type="primary"
            :disabled="smsLoading"
            @click="sendSms"
          >
            {{ smsLoading ? `${countdown}s` : "获取验证码" }}
          </van-button>
        </template>
      </van-field>

      <!-- 登录按钮 -->
      <div class="btn-wrapper">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="loginLoading"
        >
          登录
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { showToast } from "vant";
import { useRouter } from "vue-router";
import { getSmsCode, login } from "../../api/user";
import { useUserStore } from "../../store/model/user";
import { getToken } from "../../utils/token";


const defaultAvatarUrl = "/dav.png";

const router = useRouter();
const userStore = useUserStore();

/* 表单数据 */
const loginInfo = reactive({
  phone: "",
  code: ""
});

/* 状态 */
const smsLoading = ref(false);
const loginLoading = ref(false);
const countdown = ref(60);
let timer: number | null = null;

/* 校验规则 */
const mobileRules = [
  { required: true, message: "账号不能为空" },
  {
    validator: (val: string) => /^1\d{10}$/.test(val),
    message: "请输入11位手机号"
  }
];

const passwordRules = [
  { required: true, message: "密码不能为空" }
];

const codeRules = [
  { required: true, message: "验证码不能为空" }
];

/* 发送验证码 */
const sendSms = async () => {
  if (!/^1\d{10}$/.test(loginInfo.phone)) {
    showToast("请先输入正确的手机号");
    return;
  }

  smsLoading.value = true;
  countdown.value = 60;

  try{
     // 发送验证码请求
     await getSmsCode(loginInfo.phone);
       showToast("验证码已发送");

  timer = window.setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      smsLoading.value = false;
      clearInterval(timer!);
    }
   }, 1000);
  } catch(error) {
    showToast("验证码发送失败，请稍后重试");
    smsLoading.value = false;
    return;
  }
};

/* 提交登录 */
const onSubmit = async () => {
  loginLoading.value = true;


  try {
    await userStore.LoginAction({  //包含了获取用户信息的逻辑
      phone: loginInfo.phone,
      code: loginInfo.code
    });
     console.log("getToken():",getToken() );
    showToast("登录成功");
    router.replace("/home");
    

  } finally {
    loginLoading.value = false;
  }
};

</script>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: 32px 16px;
  background: #f7f8fa;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.title {
  font-size: 22px;
  font-weight: 600;
}

.subtitle {
  margin-top: 8px;
  font-size: 14px;
  color: #999;
}

.btn-wrapper {
  margin: 32px 16px 0;
}
</style>
