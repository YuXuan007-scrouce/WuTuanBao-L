//下面是 笔记作品用到的请求接口

import http from "../../utils/http";
import type { LikeCollectInfo, Comment, SendCommentDTO, Author,NoteDetailRaw, NoteActionDTO } from "./type";
import {ApiResponse} from "../index";

/*
 *上部分 根据当前用户id,查询作者信息(从显示记录跳转到加载页面时请求) 
 * 作者ID、名称、头像、是否已关注
 * 存储在 LocalStorage 中,退出后清除
 */
export function getAuthorInfo(authorId: number) {
    return http.get<Author>(`/blog2/author/${authorId}`);
}

/**
 *  点击：关注按钮|取关按钮
 *  无需返回值
 */
export function followAuthor(id: number) {
  return http.get<{followed: boolean}>(`/blog2/followed/${id}`);
}


// 中间作品展示和评论区内容
/**
 * 获取笔记详情
 */
export function getNoteDetail(blogId: number) {
    return http.get<NoteDetailRaw>(`/blog2/detail/${blogId}`);
}
/**
 * 获取评论列表
 */
export function getComments(blogId: number) {
    return http.get<Comment[]>(`/blog2/comments/${blogId}`);
}
/**
 * 点赞评论：评论ID，点赞状态
 */
export function likeComment(commentId: number, like: boolean) {
  return http.post(`/blog2/comments/${commentId}/like`, { like })
}

// 底部 发送消息
export function sendComment(payload: SendCommentDTO) {
    return http.post(`/blog2/comments/write`, payload);
}

/**
 * 底部: 点赞和收藏笔记
 */
export function likeOrCollectNote(payload: NoteActionDTO) {
   return http.post<LikeCollectInfo>(`/blog2/likecollection`, payload);
}