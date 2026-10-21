import { useId, type ReactNode } from 'react'

interface ModalProps {
  title: string
  onClose: () => void
  children: ReactNode
}

export function Modal({ title, onClose, children }: ModalProps) {
  const titleId = useId()
  return (
    <dialog open aria-labelledby={titleId} onKeyDown={(event) => event.key === 'Escape' && onClose()}>
      <article>
        <header>
          <button type="button" className="close" aria-label="Close" onClick={onClose} />
          <h3 id={titleId}>{title}</h3>
        </header>
        {children}
      </article>
    </dialog>
  )
}
