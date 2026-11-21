package com.clockmods.pro.timer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupMenu;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clockmods.R;
import java.util.List;
import java.util.function.Consumer;

/** Editable draft shared by the View and Compose dialog hosts. */
public final class SequenceEditorView extends LinearLayout {
    private final List<PomodoroSequence.Step> draft;
    private final Consumer<String> onChange;
    private final RecyclerView list;
    private final Rows rows = new Rows();
    private final ItemTouchHelper drag;
    private final Actions actions;
    private final Button add;
    private final int[] containers, onContainers;
    private AlertDialog wheelDialog;

    public SequenceEditorView(Context context, String encoded, int[] containers, int[] onContainers,
            Consumer<String> onChange, Runnable cancel, Consumer<List<PomodoroSequence.Step>> apply) {
        super(context);
        this.onChange = onChange;
        this.containers = containers;
        this.onContainers = onContainers;
        draft = PomodoroSequence.decode(encoded);
        setOrientation(VERTICAL);

        list = new RecyclerView(context);
        list.setPadding(dp(6), dp(4), dp(6), dp(4));
        list.setClipToPadding(false);
        list.setLayoutManager(new LinearLayoutManager(context));
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        animator.setMoveDuration(170);
        list.setItemAnimator(animator);
        list.setAdapter(rows);
        drag = new ItemTouchHelper(new DragPhases());
        drag.attachToRecyclerView(list);
        addView(list, new LayoutParams(-1, 0, 1));

        actions = new Actions(context);
        add = actions.action(actions.edit, R.string.pomodoro_add_step, this::addPhase);
        actions.action(actions.edit, R.string.timer_reset, this::reset);
        actions.action(actions.finish, R.string.cancel, cancel);
        Button applyButton = actions.action(actions.finish, R.string.apply, () -> apply.accept(draft));
        applyButton.setTextColor(onContainers[0]);
        applyButton.setBackground(new RippleDrawable(ColorStateList.valueOf((onContainers[0] & 0xffffff) | 0x24000000),
                pill(containers[0]), null));
        add.setEnabled(draft.size() < PomodoroSequence.MAX_SIZE);
        addView(actions, new LayoutParams(-1, -2));
    }

    public List<PomodoroSequence.Step> steps() { return draft; }

    private void addPhase() {
        int position = draft.size();
        if (position >= PomodoroSequence.MAX_SIZE) return;
        draft.add(PomodoroSequence.Step.defaultFor(0));
        rows.notifyItemInserted(position);
        if (position == PomodoroSequence.MIN_SIZE) rows.notifyItemRangeChanged(0, position);
        list.scrollToPosition(position);
        sizeChanged();
    }

    private void removePhase(int position) {
        if (position == RecyclerView.NO_POSITION || draft.size() <= PomodoroSequence.MIN_SIZE) return;
        draft.remove(position);
        rows.notifyItemRemoved(position);
        if (draft.size() == PomodoroSequence.MIN_SIZE) rows.notifyItemRangeChanged(0, draft.size());
        sizeChanged();
    }

    private void reset() {
        draft.clear();
        draft.addAll(PomodoroSequence.decode(PomodoroSequence.DEFAULT));
        rows.notifyDataSetChanged();
        list.scrollToPosition(0);
        sizeChanged();
    }

    private void sizeChanged() {
        add.setEnabled(draft.size() < PomodoroSequence.MAX_SIZE);
        requestLayout();
        publish();
    }

    private void publish() { onChange.accept(PomodoroSequence.encode(draft)); }

    private boolean move(int from, int to) {
        if (from < 0 || to < 0 || from == to || to >= draft.size()) return false;
        draft.add(to, draft.remove(from));
        rows.notifyItemMoved(from, to);
        publish();
        return true;
    }

    private void replace(PomodoroSequence.Step old, PomodoroSequence.Step next) {
        int index = draft.indexOf(old);
        if (index < 0) return;
        draft.set(index, next);
        rows.notifyItemChanged(index);
        publish();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        actions.measure(widthSpec, MeasureSpec.UNSPECIFIED);
        int desired = dp(60 * Math.min(5, draft.size()) + 8) + actions.getMeasuredHeight();
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(resolveSize(desired, heightSpec), MeasureSpec.EXACTLY));
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private ColorStateList buttonColors(int color) {
        return new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {}},
                new int[]{(color & 0xffffff) | 0x62000000, color});
    }

    private GradientDrawable pill(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(32));
        shape.setColor(color);
        return shape;
    }

    private Button button() {
        Button button = new Button(getContext(), null, android.R.attr.borderlessButtonStyle);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(3), 0, dp(3), 0);
        return button;
    }

    private ImageButton icon(int image, int description) {
        ImageButton button = new ImageButton(getContext(), null, android.R.attr.borderlessButtonStyle);
        button.setImageResource(image);
        button.setContentDescription(getResources().getString(description));
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        return button;
    }

    /** Keep each action pair together, including at larger system font scales. */
    private final class Actions extends LinearLayout {
        final LinearLayout edit, finish;
        Actions(Context context) {
            super(context);
            setPadding(dp(4), dp(12), dp(4), 0);
            edit = new LinearLayout(context);
            finish = new LinearLayout(context);
            addView(edit, new LayoutParams(-2, -2));
            addView(new android.widget.Space(context), new LayoutParams(0, 0, 1));
            LayoutParams end = new LayoutParams(-2, -2);
            end.gravity = Gravity.END;
            addView(finish, end);
        }

        Button action(LinearLayout group, int label, Runnable click) {
            Button action = button();
            action.setText(label);
            action.setMinimumWidth(dp(48));
            action.setPadding(dp(8), 0, dp(8), 0);
            action.setOnClickListener(v -> click.run());
            LayoutParams params = new LayoutParams(-2, -2);
            if (group.getChildCount() > 0) params.setMarginStart(dp(4));
            action.setMinHeight(dp(48));
            group.addView(action, params);
            return action;
        }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            edit.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            finish.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            int needed = edit.getMeasuredWidth() + finish.getMeasuredWidth() + dp(8) + getPaddingLeft() + getPaddingRight();
            setOrientation(needed <= MeasureSpec.getSize(widthSpec) ? HORIZONTAL : VERTICAL);
            super.onMeasure(widthSpec, heightSpec);
        }
    }

    private final class DragPhases extends ItemTouchHelper.SimpleCallback {
        DragPhases() { super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0); }
        @Override public boolean isLongPressDragEnabled() { return false; }
        @Override public boolean onMove(RecyclerView view, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            return move(source.getBindingAdapterPosition(), target.getBindingAdapterPosition());
        }
        @Override public void onSwiped(RecyclerView.ViewHolder holder, int direction) { }

        @Override public RecyclerView.ViewHolder chooseDropTarget(RecyclerView.ViewHolder selected,
                List<RecyclerView.ViewHolder> candidates, int x, int y) {
            View row = selected.itemView;
            int center = y + row.getHeight() / 2;
            boolean down = y > row.getTop();
            RecyclerView.ViewHolder target = null;
            int distance = 0;
            for (RecyclerView.ViewHolder candidate : candidates) {
                View other = candidate.itemView;
                int offset = other.getTop() - row.getTop();
                if ((down ? offset > 0 && center >= other.getTop() : offset < 0 && center <= other.getBottom())
                        && Math.abs(offset) > distance) {
                    target = candidate;
                    distance = Math.abs(offset);
                }
            }
            return target;
        }

        @Override public void onSelectedChanged(RecyclerView.ViewHolder holder, int state) {
            super.onSelectedChanged(holder, state);
            if (holder != null && state == ItemTouchHelper.ACTION_STATE_DRAG) {
                holder.itemView.animate().translationZ(dp(7)).setDuration(100).start();
            }
        }

        @Override public void clearView(RecyclerView view, RecyclerView.ViewHolder holder) {
            super.clearView(view, holder);
            holder.itemView.animate().translationZ(0).setDuration(110).start();
        }
    }

    private final class Rows extends RecyclerView.Adapter<PhaseRow> {
        @Override public PhaseRow onCreateViewHolder(ViewGroup parent, int type) { return new PhaseRow(); }
        @Override public int getItemCount() { return draft.size(); }
        @Override public void onBindViewHolder(PhaseRow holder, int position) { holder.bind(draft.get(position)); }
    }

    private final class PhaseRow extends RecyclerView.ViewHolder {
        final LinearLayout row;
        final ImageButton handle, remove;
        final Button label, duration;

        PhaseRow() {
            super(new LinearLayout(SequenceEditorView.this.getContext()));
            row = (LinearLayout) itemView;
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, dp(52));
            params.setMargins(0, dp(4), 0, dp(4));
            row.setLayoutParams(params);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(4), 0, dp(4), 0);
            row.setClipToOutline(true);
            handle = icon(R.drawable.sequence_drag, R.string.pomodoro_drag);
            handle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN && getBindingAdapterPosition() >= 0)
                    drag.startDrag(this);
                return true;
            });
            handle.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override public void onInitializeAccessibilityNodeInfo(View v, AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(v, info);
                    int position = getBindingAdapterPosition();
                    if (position > 0) info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
                    if (position >= 0 && position + 1 < draft.size()) info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
                }
                @Override public boolean performAccessibilityAction(View v, int action, Bundle args) {
                    int from = getBindingAdapterPosition();
                    if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) return move(from, from + 1);
                    if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) return move(from, from - 1);
                    return super.performAccessibilityAction(v, action, args);
                }
            });
            label = button();
            duration = button();
            remove = icon(R.drawable.sequence_remove, R.string.pomodoro_remove_step);
            row.addView(handle, new LayoutParams(dp(48), -1));
            row.addView(label, new LayoutParams(0, -1, 1.2f));
            row.addView(duration, new LayoutParams(0, -1, 1));
            row.addView(remove, new LayoutParams(dp(48), -1));
            remove.setOnClickListener(v -> removePhase(getBindingAdapterPosition()));
        }

        void bind(PomodoroSequence.Step step) {
            int ink = onContainers[step.label];
            row.setBackground(pill(containers[step.label]));
            handle.setImageTintList(ColorStateList.valueOf(ink));
            remove.setImageTintList(buttonColors(ink));
            remove.setEnabled(draft.size() > PomodoroSequence.MIN_SIZE);
            label.setText(TimerCompletion.phaseLabel(step.label));
            label.setTextColor(ink);
            label.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(getContext(), label);
                for (int i = 0; i < 3; i++) menu.getMenu().add(0, i, i, TimerCompletion.phaseLabel(i));
                menu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() != step.label) replace(step, PomodoroSequence.Step.defaultFor(item.getItemId()));
                    return true;
                });
                menu.show();
            });
            duration.setText(getResources().getString(R.string.timer_minutes, step.minutes));
            duration.setTextColor(ink);
            duration.setOnClickListener(v -> chooseDuration(step));
        }
    }

    private void chooseDuration(PomodoroSequence.Step step) {
        NumberPicker picker = new NumberPicker(getContext());
        picker.setMinValue(PomodoroSequence.MIN_MINUTES);
        picker.setMaxValue(PomodoroSequence.MAX_MINUTES);
        picker.setValue(step.minutes);
        picker.setWrapSelectorWheel(false);
        picker.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        for (int i = 0; i < picker.getChildCount(); i++) {
            if (picker.getChildAt(i) instanceof android.widget.EditText) {
                android.widget.EditText field = (android.widget.EditText) picker.getChildAt(i);
                field.setKeyListener(null);
                field.setShowSoftInputOnFocus(false);
            }
        }
        LinearLayout center = new LinearLayout(getContext());
        center.setGravity(Gravity.CENTER);
        center.addView(picker);
        wheelDialog = new AlertDialog.Builder(getContext()).setTitle(R.string.pomodoro_duration)
                .setView(center).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.apply, (d, which) -> {
                    replace(step, new PomodoroSequence.Step(step.label, picker.getValue()));
                }).create();
        wheelDialog.setOnDismissListener(dialog -> wheelDialog = null);
        wheelDialog.show();
    }

    @Override protected void onDetachedFromWindow() {
        if (wheelDialog != null) wheelDialog.dismiss();
        super.onDetachedFromWindow();
    }
}
