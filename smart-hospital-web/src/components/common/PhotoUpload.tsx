import { useRef, useState } from 'react'
import { Avatar, Spin, message as antMessage } from 'antd'
import { CameraOutlined, UserOutlined } from '@ant-design/icons'

const ALLOWED = ['image/jpeg', 'image/png', 'image/webp']
const MAX_MB  = 5

interface Props {
  photoUrl?: string | null
  name?: string
  size?: number
  uploading?: boolean
  onFileSelect: (file: File) => void
  editable?: boolean
}

export function PhotoUpload({
  photoUrl, name, size = 80, uploading = false, onFileSelect, editable = true,
}: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [hovered, setHovered] = useState(false)

  const initials = name
    ? name.split(' ').filter(Boolean).slice(0, 2).map(w => w[0].toUpperCase()).join('')
    : undefined

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    if (!ALLOWED.includes(file.type)) {
      antMessage.warning('Only JPEG, PNG or WebP images are allowed')
      e.target.value = ''
      return
    }
    if (file.size > MAX_MB * 1024 * 1024) {
      antMessage.warning(`Image must be under ${MAX_MB} MB`)
      e.target.value = ''
      return
    }
    onFileSelect(file)
    e.target.value = ''
  }

  const avatarContent = (
    <Avatar
      size={size}
      src={photoUrl ?? undefined}
      style={!photoUrl ? { background: '#1677ff', fontSize: size * 0.3 } : undefined}
      icon={!photoUrl && !initials ? <UserOutlined /> : undefined}
    >
      {!photoUrl && initials}
    </Avatar>
  )

  if (!editable) return avatarContent

  return (
    <div style={{ position: 'relative', display: 'inline-block' }}>
      {/* Hidden file input — label click triggers it directly */}
      <input
        ref={inputRef}
        id="photo-upload-input"
        type="file"
        accept=".jpg,.jpeg,.png,.webp"
        style={{ display: 'none' }}
        onChange={handleChange}
      />

      <label
        htmlFor="photo-upload-input"
        style={{ cursor: uploading ? 'default' : 'pointer', display: 'block' }}
        title="Click to change photo"
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
      >
        <Spin spinning={uploading}>
          <div style={{ position: 'relative', display: 'inline-block' }}>
            {avatarContent}

            {/* Overlay shown on hover */}
            {!uploading && hovered && (
              <div
                style={{
                  position: 'absolute',
                  inset: 0,
                  borderRadius: '50%',
                  background: 'rgba(0,0,0,0.45)',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 2,
                }}
              >
                <CameraOutlined style={{ color: '#fff', fontSize: size * 0.25 }} />
                <span style={{ color: '#fff', fontSize: size * 0.12, lineHeight: 1 }}>Upload</span>
              </div>
            )}
          </div>
        </Spin>
      </label>
    </div>
  )
}
