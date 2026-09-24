import * as React from "react";
import { createPortal } from "react-dom";
import { cn } from "@/lib/utils";

interface DropdownMenuContextValue {
  open: boolean;
  setOpen: React.Dispatch<React.SetStateAction<boolean>>;
  triggerRef: React.RefObject<HTMLButtonElement | null>;
  contentRef: React.RefObject<HTMLDivElement | null>;
  triggerRect: DOMRect | null;
  updateTriggerRect: () => void;
}

const DropdownMenuContext = React.createContext<DropdownMenuContextValue | null>(null);

function useDropdownMenuContext() {
  const context = React.useContext(DropdownMenuContext);
  if (!context) {
    throw new Error("Componentes do DropdownMenu devem ser usados dentro de <DropdownMenu />");
  }
  return context;
}

export interface DropdownMenuProps {
  children: React.ReactNode;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  modal?: boolean;
  dir?: "ltr" | "rtl";
  defaultOpen?: boolean;
}

const DropdownMenu: React.FC<DropdownMenuProps> = ({ children, open: controlledOpen, onOpenChange }) => {
  const [internalOpen, setInternalOpen] = React.useState(false);
  const [triggerRect, setTriggerRect] = React.useState<DOMRect | null>(null);

  const isControlled = controlledOpen !== undefined;
  const open = isControlled ? controlledOpen : internalOpen;

  const triggerRef = React.useRef<HTMLButtonElement | null>(null);
  const contentRef = React.useRef<HTMLDivElement | null>(null);

  const updateTriggerRect = React.useCallback(() => {
    if (triggerRef.current) {
      setTriggerRect(triggerRef.current.getBoundingClientRect());
    }
  }, []);

  const setOpen = React.useCallback(
    (value: React.SetStateAction<boolean>) => {
      const nextOpen = typeof value === "function" ? value(open) : value;
      if (nextOpen) {
        updateTriggerRect();
      }
      if (!isControlled) {
        setInternalOpen(nextOpen);
      }
      onOpenChange?.(nextOpen);
    },
    [isControlled, open, onOpenChange, updateTriggerRect]
  );

  React.useEffect(() => {
    if (!open) return;

    updateTriggerRect();

    function handleClickOutside(event: MouseEvent) {
      const target = event.target as Node;
      if (triggerRef.current && triggerRef.current.contains(target)) {
        return;
      }
      if (contentRef.current && contentRef.current.contains(target)) {
        return;
      }
      setOpen(false);
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setOpen(false);
      }
    }

    function handleResizeOrScroll() {
      updateTriggerRect();
    }

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleKeyDown);
    window.addEventListener("resize", handleResizeOrScroll);
    window.addEventListener("scroll", handleResizeOrScroll, true);

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("resize", handleResizeOrScroll);
      window.removeEventListener("scroll", handleResizeOrScroll, true);
    };
  }, [open, setOpen, updateTriggerRect]);

  return (
    <DropdownMenuContext.Provider
      value={{ open, setOpen, triggerRef, contentRef, triggerRect, updateTriggerRect }}
    >
      <div className="relative inline-block text-left w-full">
        {children}
      </div>
    </DropdownMenuContext.Provider>
  );
};

export interface DropdownMenuTriggerProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  asChild?: boolean;
}

const DropdownMenuTrigger = React.forwardRef<HTMLButtonElement, DropdownMenuTriggerProps>(
  ({ className, children, asChild, onClick, ...props }, forwardedRef) => {
    const { open, setOpen, triggerRef, updateTriggerRect } = useDropdownMenuContext();

    const handleRef = (node: HTMLButtonElement | null) => {
      (triggerRef as React.MutableRefObject<HTMLButtonElement | null>).current = node;
      if (typeof forwardedRef === "function") {
        forwardedRef(node);
      } else if (forwardedRef) {
        (forwardedRef as React.MutableRefObject<HTMLButtonElement | null>).current = node;
      }
    };

    const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
      updateTriggerRect();
      onClick?.(e);
      if (!e.defaultPrevented) {
        setOpen((prev) => !prev);
      }
    };

    if (asChild && React.isValidElement(children)) {
      return React.cloneElement(children as React.ReactElement<any>, {
        ref: handleRef,
        "aria-haspopup": "menu",
        "aria-expanded": open,
        onClick: (e: React.MouseEvent<HTMLButtonElement>) => {
          (children as React.ReactElement<any>).props.onClick?.(e);
          handleClick(e);
        },
      });
    }

    return (
      <button
        ref={handleRef}
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={handleClick}
        className={className}
        {...props}
      >
        {children}
      </button>
    );
  }
);
DropdownMenuTrigger.displayName = "DropdownMenuTrigger";

export interface DropdownMenuContentProps extends React.HTMLAttributes<HTMLDivElement> {
  side?: "top" | "right" | "bottom" | "left";
  align?: "start" | "center" | "end";
  sideOffset?: number;
}

const DropdownMenuContent = React.forwardRef<HTMLDivElement, DropdownMenuContentProps>(
  ({ className, children, side = "bottom", align = "start", sideOffset = 4, style: customStyle, ...props }, forwardedRef) => {
    const { open, contentRef, triggerRect } = useDropdownMenuContext();

    const handleRef = (node: HTMLDivElement | null) => {
      (contentRef as React.MutableRefObject<HTMLDivElement | null>).current = node;
      if (typeof forwardedRef === "function") {
        forwardedRef(node);
      } else if (forwardedRef) {
        (forwardedRef as React.MutableRefObject<HTMLDivElement | null>).current = node;
      }
    };

    if (!open || typeof document === "undefined") return null;

    const computedStyle: React.CSSProperties = {
      position: "fixed",
      zIndex: 99999,
      ...customStyle,
    };

    if (triggerRect) {
      if (side === "right") {
        computedStyle.left = `${triggerRect.right + sideOffset}px`;
        if (align === "end") {
          computedStyle.bottom = `${Math.max(8, window.innerHeight - triggerRect.bottom)}px`;
        } else if (align === "center") {
          computedStyle.top = `${triggerRect.top + triggerRect.height / 2}px`;
          computedStyle.transform = "translateY(-50%)";
        } else {
          computedStyle.top = `${triggerRect.top}px`;
        }
      } else if (side === "left") {
        computedStyle.right = `${window.innerWidth - triggerRect.left + sideOffset}px`;
        if (align === "end") {
          computedStyle.bottom = `${Math.max(8, window.innerHeight - triggerRect.bottom)}px`;
        } else {
          computedStyle.top = `${triggerRect.top}px`;
        }
      } else if (side === "top") {
        computedStyle.bottom = `${window.innerHeight - triggerRect.top + sideOffset}px`;
        if (align === "end") {
          computedStyle.right = `${window.innerWidth - triggerRect.right}px`;
        } else {
          computedStyle.left = `${triggerRect.left}px`;
        }
      } else {
        // bottom
        computedStyle.top = `${triggerRect.bottom + sideOffset}px`;
        if (align === "end") {
          computedStyle.right = `${window.innerWidth - triggerRect.right}px`;
        } else {
          computedStyle.left = `${triggerRect.left}px`;
        }
      }
    }

    const content = (
      <div
        ref={handleRef}
        role="menu"
        style={computedStyle}
        className={cn(
          "min-w-[8rem] overflow-hidden rounded-xl border border-fg/10 bg-surface-2 p-1 text-fg shadow-2xl animate-in fade-in zoom-in-95 duration-100 select-none",
          className
        )}
        {...props}
      >
        {children}
      </div>
    );

    return createPortal(content, document.body);
  }
);
DropdownMenuContent.displayName = "DropdownMenuContent";

export interface DropdownMenuItemProps extends React.HTMLAttributes<HTMLDivElement> {
  inset?: boolean;
  disabled?: boolean;
}

const DropdownMenuItem = React.forwardRef<HTMLDivElement, DropdownMenuItemProps>(
  ({ className, inset, disabled, onClick, ...props }, ref) => {
    const { setOpen } = useDropdownMenuContext();

    return (
      <div
        ref={ref}
        role="menuitem"
        aria-disabled={disabled}
        onClick={(e) => {
          if (disabled) return;
          onClick?.(e);
          if (!e.defaultPrevented) {
            setOpen(false);
          }
        }}
        className={cn(
          "relative flex cursor-pointer select-none items-center rounded-lg px-2.5 py-2 text-sm outline-none transition-colors hover:bg-fg/[0.1] focus:bg-fg/[0.1]",
          inset && "pl-8",
          disabled && "pointer-events-none opacity-50",
          className
        )}
        {...props}
      />
    );
  }
);
DropdownMenuItem.displayName = "DropdownMenuItem";

const DropdownMenuSeparator = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    role="separator"
    className={cn("-mx-1 my-1 h-[1px] bg-fg/[0.1]", className)}
    {...props}
  />
));
DropdownMenuSeparator.displayName = "DropdownMenuSeparator";

const DropdownMenuGroup: React.FC<React.HTMLAttributes<HTMLDivElement>> = (props) => <div {...props} />;
const DropdownMenuPortal: React.FC<{ children: React.ReactNode }> = ({ children }) => <>{children}</>;
const DropdownMenuSub: React.FC<{ children: React.ReactNode }> = ({ children }) => <>{children}</>;
const DropdownMenuRadioGroup: React.FC<{ children: React.ReactNode }> = ({ children }) => <>{children}</>;

export {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuGroup,
  DropdownMenuPortal,
  DropdownMenuSub,
  DropdownMenuRadioGroup,
};
