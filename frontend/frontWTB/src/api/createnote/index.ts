import http from "@/utils/http";
import { CreateNoteParams } from "./type";

/**
 * 创建笔记作品
 */
export const createNote = (data: FormData) => {
  return http.post('/note/create', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}


