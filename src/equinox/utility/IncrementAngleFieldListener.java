/*
 * Copyright 2018 Murat Artim (muratartim@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package equinox.utility;

import org.controlsfx.control.RangeSlider;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;

/**
 * Class for increment angle field listener.
 *
 * @author Murat Artim
 * @date Aug 6, 2014
 * @time 3:14:14 PM
 */
public class IncrementAngleFieldListener implements ChangeListener<String> {

	/** Default and interval values. */
	private static final int DEFAULT = 10, MIN = 1, MAX = 90;

	/** Angle range slider. */
	private final RangeSlider angleRange_;

	/** Listened text field. */
	protected final TextField textField_;

	/**
	 * Creates increment angle field listener.
	 *
	 * @param textField
	 *            Text field to listen.
	 * @param angleRange
	 *            Angle range slider.
	 */
	public IncrementAngleFieldListener(TextField textField, RangeSlider angleRange) {
		textField_ = textField;
		angleRange_ = angleRange;
	}

	@Override
	public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {

		try {

			// parse integer value from text
			int val = Integer.parseInt(newValue);

			// check against lower boundary
			if (val < MIN) {
				textField_.setText(Integer.toString(DEFAULT));
				angleRange_.setMinorTickCount(2);
				angleRange_.setBlockIncrement(DEFAULT);
				angleRange_.adjustLowValue(angleRange_.getLowValue());
				angleRange_.adjustHighValue(angleRange_.getHighValue());
				return;
			}

			// check against upper boundary
			if (val > MAX) {
				textField_.setText(Integer.toString(DEFAULT));
				angleRange_.setMinorTickCount(2);
				angleRange_.setBlockIncrement(DEFAULT);
				angleRange_.adjustLowValue(angleRange_.getLowValue());
				angleRange_.adjustHighValue(angleRange_.getHighValue());
				return;
			}

			// check if divisor of 30
			if (30 % val != 0) {
				textField_.setText(Integer.toString(DEFAULT));
				angleRange_.setMinorTickCount(2);
				angleRange_.setBlockIncrement(DEFAULT);
				angleRange_.adjustLowValue(angleRange_.getLowValue());
				angleRange_.adjustHighValue(angleRange_.getHighValue());
				return;
			}

			// setup range slider
			angleRange_.setMinorTickCount(30 / val - 1);
			angleRange_.setBlockIncrement(val);
			angleRange_.adjustLowValue(angleRange_.getLowValue());
			angleRange_.adjustHighValue(angleRange_.getHighValue());
		}

		// not integer
		catch (NumberFormatException e) {
			textField_.setText(Integer.toString(DEFAULT));
			angleRange_.setMinorTickCount(2);
			angleRange_.setBlockIncrement(DEFAULT);
		}
	}
}
