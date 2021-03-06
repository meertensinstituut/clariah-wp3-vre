import React from "react";
import {FormControl, FormGroup} from 'react-bootstrap';
import RemoveButton from "./remove-button";

export default class Integer extends React.Component {

    static handleValidation(value) {
        if (value.length === 0) return null;
        return RegExp(/^[0-9]*$/).test(value)
            ? "success"
            : "error";
    }

    render() {
        return (
            <div>
                <FormGroup
                    controlId="formBasicText"
                    validationState={Integer.handleValidation(this.props.value)}
                >
                    <div className="input-group mb-3">
                        <FormControl
                            type="text"
                            value={this.props.value}
                            onChange={(e) => this.props.onChange(e.target.value)}
                        />
                        <FormControl.Feedback/>
                        <div className="input-group-append">
                            <RemoveButton
                                canRemove={this.props.canRemove}
                                onRemove={this.props.onRemove}
                                nextToInput={true}
                            />
                        </div>
                    </div>
                </FormGroup>
            </div>
        );
    }
}