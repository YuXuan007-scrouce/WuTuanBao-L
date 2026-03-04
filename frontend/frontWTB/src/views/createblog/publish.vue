<template>
  <div class="publish-page">
    <!-- 顶部导航 -->
    <van-nav-bar
      title="发布笔记"
      left-arrow
      @click-left="onBack"
    />

    <!-- 内容区 -->
    <div class="content">
      <!-- 图片上传 -->
      <van-uploader
        v-model="fileList"
        multiple
        :max-count="9"
      />

      <!-- 标题 -->
      <van-field
        v-model="form.title"
        placeholder="请输入标题"
        maxlength="50"
        show-word-limit
      />

      <!-- 内容 -->
      <van-field
        v-model="form.content"
        type="textarea"
        placeholder="分享你的笔记内容..."
        rows="6"
        maxlength="1000"
        show-word-limit
      />

      <!-- 地址 -->
      <van-field
        v-model="form.address"
        placeholder="填写地址（可选）"
      />
    </div>

    <!-- 底部发布按钮 -->
    <div class="footer">
      <van-button
        type="danger"
        block
        round
        :loading="loading"
        @click="submit"
      >
        发布
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import {createNote } from '@/api/createnote/index'
const router = useRouter()

/** 表单数据 */
const form = ref({
  title: '',
  content: '',
  address: ''
})

/** 图片列表（Vant 格式） */
const fileList = ref<any[]>([])


const loading = ref(false)

const onBack = () => {
  router.back()
}


/** 发布 */
const submit = async () => {
  if (!form.value.title || !form.value.content) {
    showToast('请填写标题和内容')
    return
  }

  if (fileList.value.length === 0) {
    showToast('请至少上传一张图片')
    return
  }

  const formData = new FormData()

  // 1️⃣ 文件
  fileList.value.forEach(item => {
    if (!item.file) return  // 跳过空文件
    
    const fileType = item.file.type || ''
    
    if (fileType.startsWith('video/')) {
      formData.append('video', item.file)  // 视频
    } else if (fileType.startsWith('image/')) {
      formData.append('images', item.file)  // 图片
    } else {
      console.warn('未知文件类型：', fileType)
    }
  })

  // 添加普通字段（JSON Blob）
  const noteData = {
    title: form.value.title,
    content: form.value.content,
    address: form.value.address || ''  // 防止 undefined
  }
  
  formData.append(
    'data',
    new Blob([JSON.stringify(noteData)], { type: 'application/json' })
  )

  try {
    loading.value = true
    await createNote(formData)
    showToast('发布成功')
    router.back()
  } catch (error: any) {
    console.error('发布失败：', error)
    showToast(error.response?.data?.message || '发布失败，请重试')
  } finally {
    loading.value = false
  }
}

</script>

<style scoped>
.publish-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.footer {
  padding: 12px;
  border-top: 1px solid #f1f1f1;
}
/* 放大 uploader 整体区域 */
:deep(.van-uploader) {
  width: 100%;
}

/* 放大每个图片预览块 */
:deep(.van-uploader__preview),
:deep(.van-uploader__upload) {
 width: 30vw;
  height: 30vw;
  max-width: 140px;
  max-height: 140px;
}

/* 图片填满 */
:deep(.van-uploader__preview-image) {
  object-fit: cover;
}

</style>
