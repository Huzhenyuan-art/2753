import { ElMessage } from 'element-plus'

const EXCEL_CONTENT_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

function readBlobAsText(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(reader.error)
    reader.readAsText(blob, 'utf-8')
  })
}

function triggerBlobDownload(blob: Blob, filename: string) {
  const downloadUrl = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = downloadUrl
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(downloadUrl)
}

export async function handleBlobDownloadResponse(response: any, fallbackFilename: string) {
  const data = response.data
  if (!(data instanceof Blob)) {
    ElMessage.error('响应格式异常')
    return
  }

  if (data.type === EXCEL_CONTENT_TYPE || (data.size > 0 && !data.type.includes('json'))) {
    let filename = fallbackFilename
    try {
      const contentDisposition = (response.headers as any)?.['content-disposition']
      if (contentDisposition) {
        const match = contentDisposition.match(/filename\*?=(?:UTF-8'')?([^;]+)/i)
        if (match && match[1]) {
          filename = decodeURIComponent(match[1].trim().replace(/^["']|["']$/g, ''))
        }
      }
    } catch {}
    triggerBlobDownload(data, filename)
    ElMessage.success('下载成功')
    return
  }

  try {
    const text = await readBlobAsText(data)
    const json = JSON.parse(text)
    ElMessage.error(json.message || '下载失败')
  } catch {
    ElMessage.error('下载失败')
  }
}
