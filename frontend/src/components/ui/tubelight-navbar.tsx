import { useEffect, useState } from "react"
import { motion } from "framer-motion"
import { LucideIcon } from "lucide-react"
import { cn } from "@/lib/utils"

export interface NavItem {
  name: string
  url?: string
  id?: string
  icon: LucideIcon
  onClick?: () => void
}

export interface NavBarProps {
  items: NavItem[]
  activeId?: string
  onSelect?: (id: string) => void
  className?: string
}

export function NavBar({ items, activeId, onSelect, className }: NavBarProps) {
  const [internalActive, setInternalActive] = useState(activeId || items[0]?.id || items[0]?.name)

  useEffect(() => {
    if (activeId !== undefined) {
      setInternalActive(activeId)
    }
  }, [activeId])

  return (
    <div
      className={cn(
        "fixed bottom-4 left-1/2 -translate-x-1/2 z-50 pointer-events-auto",
        className,
      )}
    >
      <div className="flex items-center gap-1 sm:gap-2 bg-surface-2/90 border border-fg/10 backdrop-blur-xl py-1 px-1.5 sm:py-1.5 sm:px-2 rounded-full shadow-apple-elevated">
        {items.map((item) => {
          const Icon = item.icon
          const itemId = item.id || item.name
          const isActive = internalActive === itemId

          const handleClick = () => {
            setInternalActive(itemId)
            if (item.onClick) {
              item.onClick()
            }
            if (onSelect) {
              onSelect(itemId)
            }
          }

          const content = (
            <>
              <Icon size={18} strokeWidth={2.2} className="shrink-0" />
              <span className="hidden sm:inline text-xs font-semibold whitespace-nowrap">{item.name}</span>
              {isActive && (
                <motion.div
                  layoutId="lamp"
                  className="absolute inset-0 w-full bg-fg/[0.08] rounded-full -z-10"
                  initial={false}
                  transition={{
                    type: "spring",
                    stiffness: 300,
                    damping: 30,
                  }}
                >
                  <div className="absolute -top-1.5 left-1/2 -translate-x-1/2 w-8 h-0.5 bg-fg rounded-t-full">
                    <div className="absolute w-10 h-4 bg-fg/20 rounded-full blur-md -top-2 -left-1" />
                    <div className="absolute w-6 h-3 bg-fg/30 rounded-full blur-sm -top-1 left-1" />
                  </div>
                </motion.div>
              )}
            </>
          )

          const commonClasses = cn(
            "relative cursor-pointer text-xs font-medium px-3 py-2 sm:px-4 sm:py-2 rounded-full transition-colors flex items-center gap-1.5 select-none outline-none",
            "text-fg/60 hover:text-fg",
            isActive && "bg-fg/10 text-fg font-semibold shadow-sm",
          )

          if (item.url && !item.onClick && !onSelect) {
            return (
              <a
                key={itemId}
                href={item.url}
                onClick={handleClick}
                className={commonClasses}
              >
                {content}
              </a>
            )
          }

          return (
            <button
              key={itemId}
              type="button"
              onClick={handleClick}
              className={commonClasses}
            >
              {content}
            </button>
          )
        })}
      </div>
    </div>
  )
}
